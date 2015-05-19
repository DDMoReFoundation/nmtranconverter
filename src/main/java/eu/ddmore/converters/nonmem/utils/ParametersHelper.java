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
import java.util.TreeMap;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.OmegaBlockStatement;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.LinearCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This is helper class for parser to parse parameter related functionality for NmTran conversion.
 * 
 */
public class ParametersHelper {
    private static final String MU = "MU_";
    private final LinkedHashMap<String, ThetaStatement> thetaStatements = new LinkedHashMap<String, ThetaStatement>();
    private final LinkedHashMap<String, OmegaStatement> omegaStatements = new LinkedHashMap<String, OmegaStatement>();

    private final TreeMap<Integer, String> thetasToEtaOrder = new TreeMap<Integer, String>();
    private ScriptDefinition scriptDefinition;
    private List<SimpleParameter> SimpleParameters = new ArrayList<SimpleParameter>();

    // These are keyed by symbol ID
    private final Map<String, SimpleParameter> simpleParams = new HashMap<String, SimpleParameter>();
    private final Map<String, ScalarRhs> initialEstimates = new HashMap<String, ScalarRhs>();
    private final Map<String, ScalarRhs> lowerBounds = new HashMap<String, ScalarRhs>();
    private final Map<String, ScalarRhs> upperBounds = new HashMap<String, ScalarRhs>();
    private final OmegaBlockStatement omegaBlockStatement = new OmegaBlockStatement(this);
    private final List<String> verifiedSigmas = new ArrayList<String>();
    private List<ParameterEstimate> parametersToEstimate = new ArrayList<ParameterEstimate>();
    private List<FixedParameter> fixedParameters = new ArrayList<FixedParameter>();

    /**
     * Constructor expects script definition which contains all the blocks populated as part of common converter. 
     *   
     * @param scriptDefinition
     */
    public ParametersHelper(ScriptDefinition scriptDefinition){
        this.scriptDefinition = scriptDefinition;
    }

    /**
     * This method initialises all the Sigma, Omega and Theta parameter maps from simple parameters and their properties.
     * 
     * @param SimpleParameters
     */
    public void initialiseAllParameters(List<SimpleParameter> SimpleParameters, Map<String, Integer> orderedEtas){

        if (SimpleParameters==null || SimpleParameters.isEmpty()) {
            return;
        }else {
            setSimpleParameters(SimpleParameters);
            for (SimpleParameter simpleParam : SimpleParameters) {
                simpleParams.put(simpleParam.getSymbId(), simpleParam);
            }
        }

        final EstimationStep estimationStep = getEstimationStep(scriptDefinition);
        parametersToEstimate = (estimationStep.hasParametersToEstimate())?estimationStep.getParametersToEstimate(): new ArrayList<ParameterEstimate>();
        fixedParameters = (estimationStep.hasFixedParameters())?estimationStep.getFixedParameters(): new ArrayList<FixedParameter>();
        // Find any bounds and initial estimates
        setAllParameterBounds(parametersToEstimate);

        //setOmegaBlocks before omega params and theta
        omegaBlockStatement.setEtaToOmagaMap(orderedEtas);
        createOrderedThetasToEta(orderedEtas);

        //need to set omegas and sigma before setting theta params
        omegaBlockStatement.createOmegaBlocks();

        setOmegaParameters();
        setThetaParameters();
    }

    /**
     * This method will set initial theta parameters as well as simple parameters.
     * As omega parameters and constants need to be removed from this list this is not final Theta parameters list. 
     * 
     */
    private void setThetaParameters(){
        final Map<String, ThetaStatement> unOrderedThetas = new HashMap<String, ThetaStatement>();
        for(ParameterEstimate parameter : parametersToEstimate){
            String paramName = parameter.getSymbRef().getSymbIdRef();
            createThetaForValidParam(unOrderedThetas, paramName, false);
            simpleParams.remove(paramName);
        }
        for(FixedParameter fixedParameter : fixedParameters){
            String paramName = fixedParameter.pe.getSymbRef().getSymbIdRef();
            createThetaForValidParam(unOrderedThetas, paramName, true);
            simpleParams.remove(paramName);
        }
        for(String paramName : simpleParams.keySet()){
            ScalarRhs scalar = getScalarRhsForSymbol(paramName);
            if(scalar !=null){
                initialEstimates.put(paramName, scalar);
                createThetaForValidParam(unOrderedThetas, paramName, true);    
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
    private void createThetaForValidParam(final Map<String, ThetaStatement> unOrderedThetas, String paramName, Boolean isFixed) {
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
        return !(paramName== null ||  omegaStatements.containsKey(paramName) || 
                omegaBlockStatement.getEtasToOmegasInCorrelation().values().contains(paramName) ||
                verifiedSigmas.contains(paramName) || thetaStatements.containsKey(paramName));
    }

    /**
     * This method will add MU statements as per ordered theta map.
     * It will be added after individual parameter definitions.
     * @return
     */
    public StringBuilder addMUStatements(){
        StringBuilder muStatement = new StringBuilder();
        for(Integer thetaOrder : thetasToEtaOrder.keySet()){
            muStatement.append(Formatter.endline(MU+thetaOrder+" = "+NmConstant.LOG+"("+thetasToEtaOrder.get(thetaOrder)+")" ));
        }

        return muStatement;
    }

    /**
     * Add MU symbol for population parameter symbol
     * @param popSymbol
     * @return
     */
    public String getMUSymbol(String popSymbol){
        for(Integer thetaOrder: thetasToEtaOrder.keySet()){
            if(popSymbol.equals(thetasToEtaOrder.get(thetaOrder))){
                return new String(MU+thetaOrder);
            }
        }
        return new String(); 

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
                ScalarRhs scalar = getScalarRhsForSymbol(omegaSymbId);
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
    private ScalarRhs getScalarRhsForSymbol(String symbId) {
        ScalarRhs scalar = null;
        
        if(simpleParams.containsKey(symbId)){
            SimpleParameter param = simpleParams.get(symbId);
            if(simpleParams.get(symbId).getAssign().getScalar()!=null){
                scalar = ConversionContext.createScalarRhs(symbId, param.getAssign().getScalar());    
            }
        }
        return scalar;
    }

    /**
     * This method will set omega parameters apart from omega blocks.
     * 
     */
    private void setOmegaParameters(){
        for (ParameterRandomVariable rv : getRandomVarsFromParameterBlock()) {
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
     * This method returns first estimation step found in steps map from script definition.
     * 
     * @param scriptDefinition
     * @return
     */
    public static EstimationStep getEstimationStep(ScriptDefinition scriptDefinition) {
        EstimationStep step = null;
        for (Part nextStep : scriptDefinition.getStepsMap().values()) {
            if (nextStep instanceof EstimationStep) step = (EstimationStep) nextStep; 
        }
        return step;
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

    /**
     * Create ordered thetas to eta map from ordered etas map.
     * The order is used to add Thetas in order of thetas.
     */
    public void createOrderedThetasToEta(Map<String, Integer> orderedEtas){        
        for(Integer nextEtaOrder : orderedEtas.values()){
            if(thetasToEtaOrder.get(nextEtaOrder)==null || thetasToEtaOrder.get(nextEtaOrder).isEmpty()){
                addToThetasOrderMap(orderedEtas, nextEtaOrder);
            }
        }
    }

    /**
     * Creates ordered thetas list with help of etasOrderMap and individual parameters
     * @param orderedEtas
     * @param nextEtaOrder
     */
    private void addToThetasOrderMap(Map<String, Integer> orderedEtas, Integer nextEtaOrder) {
        for(ParameterBlock block : scriptDefinition.getParameterBlocks()){
            for(IndividualParameter parameterType: block.getIndividualParameters()){
                final GaussianModel gaussianModel = parameterType.getGaussianModel();
                if (gaussianModel != null) {
                    String popSymbol = getPopSymbol(gaussianModel);
                    List<ParameterRandomEffect> randomEffects = gaussianModel.getRandomEffects();
                    for (ParameterRandomEffect randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        if(orderedEtas.get(eta).equals(nextEtaOrder)){
                            thetasToEtaOrder.put(nextEtaOrder, popSymbol);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * This method gets population parameter symbol id from linear covariate of a gaussian model.
     * 
     * @param gaussianModel
     * @return
     */
    public String getPopSymbol(final GaussianModel gaussianModel) {
        LinearCovariate lcov =  gaussianModel.getLinearCovariate();
        if(lcov!=null && lcov.getPopulationParameter()!=null){
            Rhs assign = lcov.getPopulationParameter().getAssign();
            if(assign.getSymbRef()!=null)
                return assign.getSymbRef().getSymbIdRef();
            else
                return assign.getEquation().getSymbRef().getSymbIdRef();		
        }else{
            throw new IllegalArgumentException("Pop symbol missing.The population parameter is not well formed.");
        }
    }

    private List<ParameterRandomVariable> getRandomVarsFromParameterBlock() {
        List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();

        //Unless parameterBlocks is empty, getting first parameter Block.
        if(!parameterBlocks.isEmpty()){
            return parameterBlocks.get(0).getRandomVariables();
        }
        else{
            throw new IllegalStateException("parameterBlocks cannot be empty");
        }
    }

    public void addToSigmaVerificationListIfNotExists(String sigmaVar){
        if(!verifiedSigmas.contains(sigmaVar))
            verifiedSigmas.add(sigmaVar);
    }

    /**
     * Prepares theta statement for thetas if present.
     *  
     * @return omega statement
     */
    public StringBuilder getThetaStatementBlock(){
        StringBuilder thetaStatement = new StringBuilder();
        if (!thetaStatements.isEmpty()) {
            thetaStatement.append(Formatter.endline()+Formatter.theta());
            for (String thetaVar : thetaStatements.keySet()) {
                thetaStatement.append(ParameterStatementHandler.addParameter(thetaStatements.get(thetaVar)));
            }
        }
        return thetaStatement;
    }

    /**
     * Prepares omega statement using omega block and omegas if present.
     *  
     * @return omega statement
     */
    public StringBuilder getOmegaStatementBlock() {
        StringBuilder omegaStatement = new StringBuilder();
        Map<String, List<OmegaStatement>> omegaBlocks = omegaBlockStatement.getOmegaBlocks();

        if(!omegaBlocks.isEmpty()){
            omegaStatement.append(Formatter.endline(omegaBlockStatement.getOmegaBlockTitle()));
            for(String eta : omegaBlockStatement.getOrderedEtasToOmegaMap().values()){
                for(OmegaStatement omega : omegaBlocks.get(eta)){
                    omegaStatement.append(ParameterStatementHandler.addParameter(omega));
                }
            }
        }

        if (!omegaDoesNotExist()) {
            omegaStatement.append(Formatter.endline());
            omegaStatement.append(Formatter.endline(Formatter.omega()));
            for (final String omegaVar : omegaStatements.keySet()) {
                omegaStatement.append(ParameterStatementHandler.addParameter(omegaStatements.get(omegaVar)));
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

    public List<SimpleParameter> getSimpleParameters() {
        return SimpleParameters;
    }

    public void setSimpleParameters(List<SimpleParameter> SimpleParameters) {
        this.SimpleParameters = SimpleParameters;
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

    public Map<String, SimpleParameter> getSimpleParams() {
        return simpleParams;
    }

    public OmegaBlockStatement getOmegaBlockStatement() {
        return omegaBlockStatement;
    }
}
