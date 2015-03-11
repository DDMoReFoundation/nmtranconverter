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
import crx.converter.engine.parts.BaseRandomVariableBlock.Correlation;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.statements.OmegaBlockStatement;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.SigmaStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.converters.nonmem.utils.Formatter.Constant;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel.LinearCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameterType;
import eu.ddmore.libpharmml.dom.modellingsteps.InitialEstimateType;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimateType;
import eu.ddmore.libpharmml.dom.uncertml.AbstractContinuousUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

public class ParametersHelper {
    private static final String MU = "MU_";
    private final LinkedHashMap<String, ThetaStatement> thetaStatements = new LinkedHashMap<String, ThetaStatement>();
    private final LinkedHashMap<String, OmegaStatement> omegaStatements = new LinkedHashMap<String, OmegaStatement>();
    private List<String> SigmaStatements = new ArrayList<String>();

    private final TreeMap<Integer, String> thetasToEtaOrder = new TreeMap<Integer, String>();
    private ScriptDefinition scriptDefinition;
    private List<SimpleParameterType> simpleParameterTypes = new ArrayList<SimpleParameterType>();

    // These are keyed by symbol ID
    private final Map<String, SimpleParameterType> simpleParams = new HashMap<String, SimpleParameterType>();
    private final Map<String, InitialEstimateType> initialEstimates = new HashMap<String, InitialEstimateType>();
    private final Map<String, ScalarRhs> lowerBounds = new HashMap<String, ScalarRhs>();
    private final Map<String, ScalarRhs> upperBounds = new HashMap<String, ScalarRhs>();
    private List<ParameterEstimateType> parametersToEstimate = new ArrayList<ParameterEstimateType>();
    private List<FixedParameter> fixedParameters = new ArrayList<FixedParameter>();
    private OmegaBlockStatement omegaBlockStatement = new OmegaBlockStatement(this);
    private List<String> sigmas = new ArrayList<String>();

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
     * @param simpleParameterTypes
     */
    public void initialiseSimpleParams(List<SimpleParameterType> simpleParameterTypes){

        if (simpleParameterTypes==null || simpleParameterTypes.isEmpty()) {
            return;
        }else {
            setSimpleParameterTypes(simpleParameterTypes);
        }

        final EstimationStep estimationStep = getEstimationStep(scriptDefinition);
        parametersToEstimate = (estimationStep.hasParametersToEstimate())?estimationStep.getParametersToEstimate(): new ArrayList<ParameterEstimateType>();
        fixedParameters = (estimationStep.hasFixedParameters())?estimationStep.getFixedParameters(): new ArrayList<FixedParameter>();
        // Find any bounds and initial estimates
        setAllParameterBounds(parametersToEstimate);

        //setOmegaBlocks before omega params and theta
        omegaBlockStatement.setEtaToOmagaMap(createOrderedEtasMap());
        createOrderedThetasToEta();

        //need to set omegas and sigma before setting theta params
        omegaBlockStatement.createOmegaBlocks();

        setOmegaParameters();
        setSigmaParameters();
        setThetaParameters();
    }

    /**
     * This method will set initial theta parameters as well as simple parameters.
     * As omega parameters and constants need to be removed from this list this is not final Theta parameters list. 
     * 
     */
    private void setThetaParameters(){
        for (SimpleParameterType simpleParam : getSimpleParameterTypes()) {
            simpleParams.put(simpleParam.getSymbId(), simpleParam);
        }

        final Map<String, ThetaStatement> unOrderedThetas = new HashMap<String, ThetaStatement>();
        for(ParameterEstimateType parameter : parametersToEstimate){
            String paramName = parameter.getSymbRef().getSymbIdRef();
            createThetaForValidParam(unOrderedThetas, paramName, false);
        }
        for(FixedParameter fixedParameter : fixedParameters){
            String paramName = fixedParameter.p.getSymbRef().getSymbIdRef();
            createThetaForValidParam(unOrderedThetas, paramName, true);
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
        thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
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
                sigmas.contains(paramName) || thetaStatements.containsKey(paramName));
    }

    /**
     * This method will add MU statements as per ordered theta map.
     * It will be added after individual parameter definitions.
     * @return
     */
    public StringBuilder addMUStatements(){
        StringBuilder muStatement = new StringBuilder();
        for(Integer thetaOrder : thetasToEtaOrder.keySet()){
            muStatement.append(Formatter.endline(MU+thetaOrder+" = "+Constant.LOG+"("+thetasToEtaOrder.get(thetaOrder)+")" ));
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
        }
        return omegaStatement;
    }

    private void setSigmaParameters(){
        SigmaStatementBuilder sigmaStatementBuilder = new SigmaStatementBuilder();
        SigmaStatements = sigmaStatementBuilder.getSigmaStatements(this);
    }

    /**
     * This method will set omega parameters apart from omega blocks.
     * 
     */
    private void setOmegaParameters(){
        for (ParameterRandomVariableType rv : getRandomVarsFromParameterBlock()) {
            String symbId = getNameFromParamRandomVariable(rv);
            OmegaStatement omegaStatement = getOmegaFromRandomVarName(symbId);
            if(omegaStatement!=null){
                for(Iterator<FixedParameter> it= fixedParameters.iterator();it.hasNext();){
                    String paramName = it.next().p.getSymbRef().getSymbIdRef();
                    if(paramName.equals(symbId)){
                        omegaStatement.setFixed(true);
                        it.remove();
                    }
                }
                if(isParamFromStdDev(rv)){
                    omegaStatement.setStdDev(true);
                }
                omegaStatements.put(symbId, omegaStatement);
            }
        }
    }

    /**
     * Returns parameter name from parameter random variable. 
     * The name can be obtained from either standard deviation or variance. 
     * @param rv
     * @return
     */
    public String getNameFromParamRandomVariable(ParameterRandomVariableType rv) {
        String symbId = null;
        if (getDistributionTypeStdDev(rv) != null) {
            symbId = getDistributionTypeStdDev(rv).getVar().getVarId();					
        } else if (getDistributionTypeVariance(rv) != null) {
            symbId = getDistributionTypeVariance(rv).getVar().getVarId();
        }
        return symbId;
    }

    /**
     * Identifies if current parameter is from Standard Deviation.
     * 
     * @param rv
     * @return
     */
    public Boolean isParamFromStdDev(ParameterRandomVariableType rv) {
        if (getDistributionTypeStdDev(rv) != null) {
            return true;
        } else if (getDistributionTypeVariance(rv) != null) {
            return false;
        }else{
            throw new IllegalStateException("Distribution type for variable "+rv.getSymbId()+" is unknown");
        }
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public PositiveRealValueType getDistributionTypeStdDev(ParameterRandomVariableType rv){
        final AbstractContinuousUnivariateDistributionType distributionType = rv.getAbstractContinuousUnivariateDistribution().getValue();
        if (distributionType instanceof NormalDistribution) {
            return ((NormalDistribution) distributionType).getStddev();
        }
        return null;
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public PositiveRealValueType getDistributionTypeVariance(ParameterRandomVariableType rv){
        final AbstractContinuousUnivariateDistributionType distributionType = rv.getAbstractContinuousUnivariateDistribution().getValue();
        if (distributionType instanceof NormalDistribution) {
            return ((NormalDistribution) distributionType).getVariance();
        }
        return null;
    }

    /**
     * This method sets all maps for lower and upper bounds as well as for initial estimates from parameters to estimate.
     *  
     * @param parametersToEstimate
     */
    private void setAllParameterBounds(List<ParameterEstimateType> parametersToEstimate) {
        for (ParameterEstimateType paramEstimate : parametersToEstimate) {
            setParameterBounds(paramEstimate);
        }
        for (FixedParameter fixedParameter : fixedParameters){
            setParameterBounds(fixedParameter.p);
        }
    }

    private void setParameterBounds(ParameterEstimateType paramEstimate){
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
    public List<ParameterEstimateType> getAllEstimationParams(){
        List<ParameterEstimateType> allParams = new ArrayList<ParameterEstimateType>();
        allParams.addAll(getParametersToEstimate());
        for(FixedParameter fixedParam : fixedParameters){
            allParams.add(fixedParam.p);
        }
        return allParams;
    }

    /**
     * Checks the order of Etas and use this order while arranging Omega blocks.
     * This method will create map for EtaOrder which will be order for respective Omegas as well.
     * @return 
     */
    public Map<String, Integer> createOrderedEtasMap(){
        LinkedHashMap<String, Integer> etasOrderMap = new LinkedHashMap<String, Integer>();
        //We need to have this as list as this will retains order of etas
        List<String> etasOrder = getAllEtas(scriptDefinition);

        if(!etasOrder.isEmpty()){
            Integer etaCount = 0;
            Map<String, String> etaTocorrelationsMap = addCorrelationValuesToMap(getAllCorrelations());

            List<String> nonOmegaBlockEtas = new ArrayList<String>();
            //order etas map
            for(String eta : etasOrder) {
                //no correlations so no Omega block
                if(etaTocorrelationsMap.keySet().contains(eta)){
                    ++etaCount;
                    etasOrderMap.put(eta,etaCount);
                }else{
                    nonOmegaBlockEtas.add(eta);
                }
            }
            for(String nonOmegaEta : nonOmegaBlockEtas){
                etasOrderMap.put(nonOmegaEta, ++etaCount);
            }
        }

        return etasOrderMap;
    }

    /**
     * This is helper method to create ordered Etas map which gets all etas list from individual parameters. 
     * @return
     */
    public static List<String> getAllEtas(ScriptDefinition scriptDefinition) {
        List<String> etasOrder = new ArrayList<String>();
        List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();

        for(ParameterBlock block : blocks ){
            for(IndividualParameterType parameterType: block.getIndividualParameters()){
                if (parameterType.getGaussianModel() != null) {
                    List<ParameterRandomEffectType> randomEffects = parameterType.getGaussianModel().getRandomEffects();
                    for (ParameterRandomEffectType randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        if(!etasOrder.contains(eta)){
                            etasOrder.add(eta);
                        }
                    }
                }
            }
        }
        return etasOrder;
    }

    /**
     * Create ordered thetas to eta map from ordered etas map.
     * The order is used to add Thetas in order of thetas.
     */
    public void createOrderedThetasToEta(){
        Map<String, Integer> etasOrderMap = createOrderedEtasMap();
        for(Integer nextEtaOrder : etasOrderMap.values()){
            if(thetasToEtaOrder.get(nextEtaOrder)==null || thetasToEtaOrder.get(nextEtaOrder).isEmpty()){
                addToThetasOrderMap(etasOrderMap, nextEtaOrder);
            }
        }
    }

    /**
     * Creates ordered thetas list with help of etasOrderMap and individual parameters
     * @param etasOrderMap
     * @param nextEtaOrder
     */
    private void addToThetasOrderMap(Map<String, Integer> etasOrderMap, Integer nextEtaOrder) {
        for(ParameterBlock block : scriptDefinition.getParameterBlocks()){
            for(IndividualParameterType parameterType: block.getIndividualParameters()){
                final GaussianModel gaussianModel = parameterType.getGaussianModel();
                if (gaussianModel != null) {
                    String popSymbol = getPopSymbol(gaussianModel);
                    List<ParameterRandomEffectType> randomEffects = gaussianModel.getRandomEffects();
                    for (ParameterRandomEffectType randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        if(etasOrderMap.get(eta).equals(nextEtaOrder)){
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

    public LinkedHashMap<String, String> addCorrelationValuesToMap(List<Correlation> correlations) {
        //We need to have it as linked hash map so that order in which correlations are added to map will be retained.
        LinkedHashMap<String, String> etaTocorrelationsMap = new LinkedHashMap<String, String>();
        for(Correlation correlation : correlations){
            addCorrelationToMap(etaTocorrelationsMap,correlation);	
        }
        return etaTocorrelationsMap;
    }

    public void addCorrelationToMap(Map<String, String> etaTocorrelationsMap, Correlation correlation) {
        String firstVar = correlation.rnd1.getSymbId();			
        String secondVar = correlation.rnd2.getSymbId();
        String coefficient = correlation.correlationCoefficient.getSymbRef().getSymbIdRef();
        //add to correlations map			
        etaTocorrelationsMap.put(firstVar,getNameFromParamRandomVariable(correlation.rnd1));
        etaTocorrelationsMap.put(secondVar,getNameFromParamRandomVariable(correlation.rnd2));
        etaTocorrelationsMap.put(coefficient,coefficient);
    }

    /**
     * Collects correlations from all the prameter blocks. 
     * 
     * @return
     */
    public List<Correlation> getAllCorrelations() {
        List<Correlation> correlations = new ArrayList<Correlation>();
        List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                correlations.addAll(block.getCorrelations());				
            }
        }
        return correlations;
    }

    private List<ParameterRandomVariableType> getRandomVarsFromParameterBlock() {
        List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();

        //Unless parameterBlocks is empty, getting first parameter Block.
        if(!parameterBlocks.isEmpty()){
            return parameterBlocks.get(0).getRandomVariables();
        }
        else{
            throw new IllegalStateException("parameterBlocks cannot be empty");
        }
    }

    public void addToSigmaList(String sigmaVar){
        if(!sigmas.contains(sigmaVar))
            sigmas.add(sigmaVar);
    }

    //Getters and setters

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public List<SimpleParameterType> getSimpleParameterTypes() {
        return simpleParameterTypes;
    }

    public void setSimpleParameterTypes(List<SimpleParameterType> simpleParameterTypes) {
        this.simpleParameterTypes = simpleParameterTypes;
    }

    public List<ParameterEstimateType> getParametersToEstimate() {
        return parametersToEstimate;
    }

    public LinkedHashMap<String, ThetaStatement> getThetaParams() {
        return thetaStatements;
    }

    public LinkedHashMap<String, OmegaStatement> getOmegaParams() {
        return omegaStatements;
    }

    public Map<String, SimpleParameterType> getSimpleParams() {
        return simpleParams;
    }

    public OmegaBlockStatement getOmegaBlockStatement() {
        return omegaBlockStatement;
    }

    public List<String> getSigmaStatements() {
        return SigmaStatements;
    }
}
