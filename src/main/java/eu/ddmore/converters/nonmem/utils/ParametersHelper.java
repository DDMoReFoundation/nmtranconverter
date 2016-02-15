/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.statements.OmegaBlock;
import eu.ddmore.converters.nonmem.statements.OmegaBlockStatement;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.SigmaStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This is helper class for parser to parse parameter related functionality for NmTran conversion.
 * 
 */
public class ParametersHelper {
    private final LinkedHashMap<String, ThetaStatement> thetaStatements = new LinkedHashMap<String, ThetaStatement>();
    private final LinkedHashMap<String, OmegaStatement> omegaStatements = new LinkedHashMap<String, OmegaStatement>();

    private Map<Integer, String> thetasToEtaOrder;
    private ScriptDefinition scriptDefinition;

    // These are keyed by symbol ID
    private final Map<String, PopulationParameter> simpleParams = new HashMap<String, PopulationParameter>();
    private final Map<String, PopulationParameter> simpleParamsWithAssignment = new LinkedHashMap<String, PopulationParameter>();
    private final Map<String, Rhs> initialEstimates = new HashMap<String, Rhs>();
    private final Map<String, Rhs> lowerBounds = new HashMap<String, Rhs>();
    private final Map<String, Rhs> upperBounds = new HashMap<String, Rhs>();
    private final OmegaBlockStatement omegaBlockStatement;
    private final List<String> verifiedSigmas = new ArrayList<String>();
    private final Set<ParameterRandomVariable> epsilonVars;
    private final CorrelationHandler correlationHandler;
    private List<ParameterEstimate> parametersToEstimate;
    private List<FixedParameter> fixedParameters;
    private StringBuilder sigmaStatement;
    InterOccVariabilityHandler iovHandler;

    /**
     * Constructor expects script definition which contains all the blocks populated as part of common converter. 
     *   
     * @param scriptDefinition
     */
    public ParametersHelper(ConversionContext context){
        this.scriptDefinition = context.getScriptDefinition();
        this.correlationHandler = context.getCorrelationHandler();
        this.iovHandler = context.getIovHandler();
        epsilonVars = ScriptDefinitionAccessor.getEpsilonRandomVariables(getScriptDefinition());
        thetasToEtaOrder = context.getOrderedThetasHandler().getOrderedThetas();

        omegaBlockStatement = new OmegaBlockStatement(this, correlationHandler, iovHandler);
    }

    /**
     * This method initialises all the Sigma, Omega and Theta parameter maps from simple parameters and their properties.
     * 
     * @param simpleParameters
     */
    public void initialiseAllParameters(List<PopulationParameter> simpleParameters){

        if (simpleParameters==null || simpleParameters.isEmpty()) {
            return;
        }else {
            for (PopulationParameter simpleParam : simpleParameters) {
                if(simpleParam.getAssign()!=null){
                    simpleParamsWithAssignment.put(simpleParam.getSymbId(), simpleParam);
                }else{
                    simpleParams.put(simpleParam.getSymbId(), simpleParam);
                }
            }
        }

        final EstimationStep estimationStep = ScriptDefinitionAccessor.getEstimationStep(scriptDefinition);
        parametersToEstimate = (estimationStep.hasParametersToEstimate())?estimationStep.getParametersToEstimate(): new ArrayList<ParameterEstimate>();
        fixedParameters = (estimationStep.hasFixedParameters())?estimationStep.getFixedParameters(): new ArrayList<FixedParameter>();

        // Find any bounds and initial estimates
        setAllParameterBounds(parametersToEstimate);

        //setOmegaBlocks before omega params and theta
        omegaBlockStatement.createOmegaBlocks();

        //need to set omegas and sigma before setting theta params
        setOmegaParameters();
        setSigmaParameters();
        setThetaParameters();
    }

    private void setSigmaParameters(){
        SigmaStatementBuilder sigmaBuilder = new SigmaStatementBuilder(this);
        sigmaStatement = sigmaBuilder.getSigmaStatementBlock();
    }

    /**
     * This method will set initial theta parameters as well as simple parameters.
     * As omega parameters and constants need to be removed from this list this is not final Theta parameters list. 
     * 
     */
    private void setThetaParameters(){
        final Map<String, ThetaStatement> unOrderedThetas = new HashMap<String, ThetaStatement>();
        for(ParameterEstimate parameter : parametersToEstimate){
            Preconditions.checkNotNull(parameter.getSymbRef(), "Parameter to estimate doesnt have parameter symbol.");
            String paramName = parameter.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, false);
            simpleParams.remove(paramName);
        }
        for(FixedParameter fixedParameter : fixedParameters){
            Preconditions.checkNotNull(fixedParameter.pe.getSymbRef(), "Fixed Parameter doesnt have parameter symbol.");
            String paramName = fixedParameter.pe.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);
            simpleParams.remove(paramName);
        }
        for(String paramName : simpleParams.keySet()){
            Rhs scalar = getScalarRhsForSymbol(paramName);
            if(scalar !=null){
                initialEstimates.put(paramName, scalar);
                createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);    
            }
        }
        thetaStatements.putAll(unOrderedThetas);
    }

    /**
     * Creates thetas for parameter name passed after verifying if its valid theta and adds it to ordered thetas map.
     *  
     * @param unOrderedThetas
     * @param paramName
     */
    private void createAndAddThetaForValidParamToMap(final Map<String, ThetaStatement> unOrderedThetas, String paramName, Boolean isFixed) {
        if(validateParamName(paramName)){
            ThetaStatement thetaStatement = new ThetaStatement(paramName);
            if(!thetasToEtaOrder.containsValue(paramName)){
                addThetaToMap(unOrderedThetas, thetaStatement,isFixed);
            }
            addThetaToMap(thetaStatements, thetaStatement,isFixed);
        }
    }

    /**
     * Creates Theta for the parameter name provided and add it to theta statement map 
     * @param unOrderedThetas
     * @param paramName
     * @param isFixed
     */
    private void addThetaToMap(final Map<String, ThetaStatement> unOrderedThetas, ThetaStatement thetaStatement, Boolean isFixed) {
        String paramName = thetaStatement.getSymbId();
        if(initialEstimates.get(paramName)==null && lowerBounds.get(paramName)==null && upperBounds.get(paramName)==null ){
            return;
        }else{
            thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
        }
        if(isFixed!=null) thetaStatement.setFixed(isFixed);
        unOrderedThetas.put(paramName, thetaStatement);
    }

    /**
     * Validate parameter before adding it to Theta, by checking if it is omega or sigma or already added to theta.
     * 
     * @param paramName
     * @return
     */
    private boolean validateParamName(String paramName) {
        boolean isValid = true;
        if(paramName== null ||  omegaStatements.containsKey(paramName) 
                || verifiedSigmas.contains(paramName) || thetaStatements.containsKey(paramName)
                || validateThetaParamForCorrOmegas(paramName, correlationHandler.getOmegaBlocksInIOV()) 
                || validateThetaParamForCorrOmegas(paramName, correlationHandler.getOmegaBlocksInNonIOV())){
            isValid = false;
        }

        return isValid;
    }

    private boolean validateThetaParamForCorrOmegas(String paramName, List<OmegaBlock> omegaBlocks) {
        boolean isValid = false;
        for(OmegaBlock omegaBlock :omegaBlocks){
            isValid = omegaBlock.getEtasToOmegas().values().contains(paramName);
            if(isValid){
                return isValid;
            }
        }
        return isValid;
    }

    /**
     * Get omega object from random variable provided.
     * This will set omega symb id as well as bounds if there are any.
     * 
     * @param randomVar
     * @return
     */
    public OmegaStatement getOmegaFromRandomVarName(String omegaSymbId) {
        OmegaStatement omegaStatement = null;
        if(omegaSymbId!= null){
            omegaStatement = new OmegaStatement(omegaSymbId);
            omegaStatement.setInitialEstimate(initialEstimates.get(omegaSymbId));

            if(initialEstimates.get(omegaSymbId) == null){
                Rhs scalar = getScalarRhsForSymbol(omegaSymbId);
                if(scalar!=null){
                    omegaStatement.setInitialEstimate(scalar);
                    omegaStatement.setFixed(true);
                }
                simpleParams.remove(omegaSymbId);
            }
        }
        return omegaStatement;
    }

    /**
     * Retrieves scalar rhs from parameter assignment with help of symbol id.
     * @param symbId
     * @return
     */
    private Rhs getScalarRhsForSymbol(String symbId) {
        Rhs scalar = null;

        if(simpleParams.containsKey(symbId)){
            PopulationParameter param = simpleParams.get(symbId);
            if(simpleParams.get(symbId).getAssign().getScalar()!=null){
                scalar = createScalarRhs(symbId, param.getAssign().getScalar());    
            }
        }
        return scalar;
    }

    /**
     * This method will create scalar Rhs object for a symbol from the scalar value provided.
     *  
     * @param symbol
     * @param scalar
     * @return ScalarRhs object
     */
    private Rhs createScalarRhs(String symbol,Scalar scalar) {
        Rhs rhs = new Rhs();
        rhs.setScalar(scalar);
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);
        rhs.setSymbRef(symbRef);
        return rhs;
    }

    /**
     * This method will set omega parameters apart from omega blocks.
     * 
     */
    private void setOmegaParameters(){
        for (ParameterRandomVariable rv : getRandomVarsFromParameterBlock()) {

            if(!iovHandler.isRandomVarIOV(rv)){
                String symbId = RandomVariableHelper.getNameFromParamRandomVariable(rv);
                OmegaStatement omegaStatement = getOmegaFromRandomVarName(symbId);
                if(omegaStatement!=null){
                    for(Iterator<FixedParameter> it= fixedParameters.iterator();it.hasNext();){
                        String paramName = it.next().pe.getSymbRef().getSymbIdRef();
                        if(paramName.equals(symbId)){
                            omegaStatement.setFixed(true);
                            it.remove();
                        }
                    }
                    if(RandomVariableHelper.isParamFromStdDev(rv)){
                        omegaStatement.setStdDev(true);
                    }
                    omegaStatements.put(symbId, omegaStatement);
                }
            }
        }
    }

    /**
     * This method sets all maps for lower and upper bounds as well as for initial estimates from parameters to estimate.
     *  
     * @param parametersToEstimate
     */
    private void setAllParameterBounds(List<ParameterEstimate> parametersToEstimate) {
        for (ParameterEstimate paramEstimate : parametersToEstimate) {
            setParameterBounds(paramEstimate);
            simpleParams.remove(paramEstimate.getSymbRef().getSymbIdRef());
        }
        for (FixedParameter fixedParameter : fixedParameters){
            setParameterBounds(fixedParameter.pe);
            simpleParams.remove(fixedParameter.pe.getSymbRef().getSymbIdRef());
        }
    }

    private void setParameterBounds(ParameterEstimate paramEstimate){
        String symbolId = paramEstimate.getSymbRef().getSymbIdRef();
        initialEstimates.put(symbolId, paramEstimate.getInitialEstimate());
        lowerBounds.put(symbolId, paramEstimate.getLowerBound());
        upperBounds.put(symbolId, paramEstimate.getUpperBound());
    }

    /**
     * We will need all the estimation parameters to identify sigma params  and at other place, 
     * irrespective of whether they are fixed or not.
     * 
     * @return
     */
    public List<ParameterEstimate> getAllEstimationParams(){
        List<ParameterEstimate> allParams = new ArrayList<ParameterEstimate>();
        allParams.addAll(getParametersToEstimate());
        for(FixedParameter fixedParam : fixedParameters){
            allParams.add(fixedParam.pe);
        }
        return allParams;
    }

    private List<ParameterRandomVariable> getRandomVarsFromParameterBlock() {
        List<ParameterRandomVariable> randomVariables = new ArrayList<ParameterRandomVariable>();

        //Unless parameterBlocks is empty, getting first parameter Block.
        if(!getScriptDefinition().getParameterBlocks().isEmpty()){
            List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
            for(ParameterBlock block : parameterBlocks){
                for(ParameterRandomVariable variable : block.getRandomVariables()){
                    if(!isResidualError(variable.getSymbId())){
                        randomVariables.add(variable);
                    }
                }
            }
        } else{
            throw new IllegalStateException("parameterBlocks cannot be empty");
        }
        return randomVariables;
    }

    private boolean isResidualError(String symbId) {
        for(ParameterRandomVariable randomVariable : getEpsilonVars()){
            if(randomVariable.getSymbId().equals(symbId)){
                return true;  
            }
        }
        return false;
    }

    /**
     * Adds identified sigma variables to verified sigmas list. 
     * @param sigmaVar
     */
    public void addToSigmaVerificationListIfNotExists(String sigmaVar){
        if(!verifiedSigmas.contains(sigmaVar))
            verifiedSigmas.add(sigmaVar);
    }

    public String getSigmaStatementBlock() {
        return sigmaStatement.toString();
    }
    /**
     * Prepares theta statement for thetas if present.
     *  
     * @return omega statement
     */
    public String getThetaStatementBlock(){
        StringBuilder thetaStatement = new StringBuilder();
        if (!thetaStatements.isEmpty()) {
            thetaStatement.append(Formatter.endline()+Formatter.theta());
            for (String thetaVar : thetaStatements.keySet()) {
                thetaStatement.append(ParameterStatementHandler.addParameter(thetaStatements.get(thetaVar)));
            }
        }
        return thetaStatement.toString();
    }

    /**
     * Prepares omega statement using omega block and omegas if present.
     *  
     * @return omega statement
     */
    public String getOmegaStatementBlock() {
        StringBuilder omegaStatement = new StringBuilder();
        List<OmegaBlock> omegaBlocks = omegaBlockStatement.getOmegaBlocksInNonIOV();

        if(!omegaBlocks.isEmpty()){
            omegaStatement.append(appendOmegaBlocks(omegaBlocks));
        }

        if (!omegaDoesNotExist()) {
            omegaStatement.append(Formatter.endline());
            omegaStatement.append(Formatter.endline(Formatter.omega()));
            for (final String omegaVar : omegaStatements.keySet()) {
                omegaStatement.append(ParameterStatementHandler.addParameter(omegaStatements.get(omegaVar)));
            }
        }
        return omegaStatement.toString();
    }

    /**
     * Prepares omega statement for IOV omega blocks if present.
     *  
     * @return omega statement
     */
    public String getOmegaStatementBlockForIOV(){
        StringBuilder omegaStatement = new StringBuilder();
        List<OmegaBlock> omegaBlocks = omegaBlockStatement.getOmegaBlocksInIOV();
        if(!omegaBlocks.isEmpty()){
            omegaStatement.append(appendOmegaBlocks(omegaBlocks));
        }

        return omegaStatement.toString();
    }

    private StringBuilder appendOmegaBlocks(List<OmegaBlock> omegaBlocks) {
        StringBuilder omegaStatement = new StringBuilder();
        for(OmegaBlock omegaBlock : omegaBlocks){
            if(omegaBlock.getOrderedEtas().size()>0){
                omegaStatement.append(Formatter.endline(omegaBlock.getOmegaBlockTitle()));
                for(Eta eta : omegaBlock.getOrderedEtas()){
                    for(OmegaStatement omega : omegaBlock.getOmegaStatements().get(eta)){
                        if(omega!=null){
                            omegaStatement.append(ParameterStatementHandler.addParameter(omega));
                        }
                    }
                }
                if(omegaBlock.isIOV()){
                    omegaStatement.append(Formatter.endline(omegaBlock.getOmegaBlockSameTitle()));
                }
            }
        }
        return omegaStatement;
    }

    /**
     * Checks if omega statements list has any omega statements added
     * 
     * @return
     */
    public Boolean omegaDoesNotExist(){
        return (omegaStatements == null || omegaStatements.isEmpty());
    }

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public List<ParameterEstimate> getParametersToEstimate() {
        return parametersToEstimate;
    }

    public LinkedHashMap<String, ThetaStatement> getThetaParams() {
        return thetaStatements;
    }

    public LinkedHashMap<String, OmegaStatement> getOmegaParams() {
        return omegaStatements;
    }

    public Map<String, PopulationParameter> getSimpleParams() {
        return simpleParams;
    }

    public OmegaBlockStatement getOmegaBlockStatement() {
        return omegaBlockStatement;
    }

    public Map<String, PopulationParameter> getSimpleParamsWithAssignment() {
        return simpleParamsWithAssignment;
    }

    public Set<ParameterRandomVariable> getEpsilonVars() {
        return epsilonVars;
    }
}
