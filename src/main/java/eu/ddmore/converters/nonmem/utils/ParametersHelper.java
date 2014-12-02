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
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
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
	
	private static final String LOG = "LOG";
	private static final String MU = "MU_";
	public static final String CORRELATION = "CORRELATION";
	public static final String FIX = "FIX";
	public static final String SD = "SD";
	
	ScriptDefinition scriptDefinition;
	List<SimpleParameterType> simpleParameterTypes = new ArrayList<SimpleParameterType>();
	final LinkedHashMap<String, ThetaStatement> thetaStatements = new LinkedHashMap<String, ThetaStatement>();
	final LinkedHashMap<String, OmegaStatement> OmegaStatements = new LinkedHashMap<String, OmegaStatement>();
	final TreeMap<Integer, String> thetasToEtaOrder = new TreeMap<Integer, String>();
	
	// These are keyed by symbol ID
	final Map<String, SimpleParameterType> simpleParams = new HashMap<String, SimpleParameterType>();
	List<ParameterEstimateType> parametersToEstimate = new ArrayList<ParameterEstimateType>();
	List<FixedParameter> fixedParameters = new ArrayList<FixedParameter>();
	final Map<String, InitialEstimateType> initialEstimates = new HashMap<String, InitialEstimateType>();
	final Map<String, ScalarRhs> lowerBounds = new HashMap<String, ScalarRhs>();
	final Map<String, ScalarRhs> upperBounds = new HashMap<String, ScalarRhs>();
	OmegaBlockStatement omegaBlockStatement = new OmegaBlockStatement(this);

	public void getParameters(List<SimpleParameterType> simpleParameterTypes){
				
		if (simpleParameterTypes.isEmpty()) {
			return;
		}else {
			setSimpleParameterTypes(simpleParameterTypes);
		}
		
		final EstimationStep estimationStep = getEstimationStep(scriptDefinition);
		parametersToEstimate = (estimationStep.hasParametersToEstimate())? estimationStep.getParametersToEstimate():new ArrayList<ParameterEstimateType>();
		fixedParameters = (estimationStep.hasFixedParameters())? estimationStep.getFixedParameters():new ArrayList<FixedParameter>();
		// Find any bounds and initial estimates
		setAllParameterBounds(parametersToEstimate);
		
		//setOmegaBlocks before omega params and theta
		omegaBlockStatement.setEtaToOmagaMap(createOrderedEtasMap());
		createOrderedThetasToEtaMap();
		
		//need to set omegas before setting theta params
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
		for (SimpleParameterType simpleParam : getSimpleParameterTypes()) {
			simpleParams.put(simpleParam.getSymbId(), simpleParam);
		}

		final Map<String, ThetaStatement> thetaStatementMap = new HashMap<String, ThetaStatement>();
		for(ParameterEstimateType parameter : parametersToEstimate){
			String paramName = parameter.getSymbRef().getSymbIdRef();
			if(validateParamName(paramName)){
				if(!thetasToEtaOrder.containsValue(paramName)){
					addThetaToMap(thetaStatementMap, paramName,false);
				}
				addThetaToMap(thetaStatements, paramName,false);
			}
		}
		for(FixedParameter fixedParameter : fixedParameters){
			String paramName = fixedParameter.p.getSymbRef().getSymbIdRef();
			if(validateParamName(paramName)){
				if(!thetasToEtaOrder.containsValue(paramName)){
					addThetaToMap(thetaStatementMap, paramName,true);
				}
				addThetaToMap(thetaStatements, paramName,true);
			}
		}
		thetaStatements.putAll(thetaStatementMap);
	}

	private void addThetaToMap(final Map<String, ThetaStatement> thetaStatementMap, String paramName, Boolean isFixed) {
			ThetaStatement thetaStatement = new ThetaStatement(paramName);
			thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
			if(isFixed!=null) thetaStatement.setFixed(isFixed);
			thetaStatementMap.put(paramName, thetaStatement);
	}

	/**
	 * Validate parameter before adding it to Theta, by checking if it is omega or sigma or already added to theta.
	 * 
	 * 
	 * @param paramName
	 * @return
	 */
	private boolean validateParamName(String paramName) {
		return !(paramName== null ||  OmegaStatements.containsKey(paramName) || 
				omegaBlockStatement.getEtasToOmegasInCorrelation().values().contains(paramName) || 
				paramName.startsWith("sigma") || thetaStatements.containsKey(paramName));
	}
	
	/**
	 * This method will add MU statements as per ordered thets map.
	 * It will be added after individual parameter definitions.
	 * @return
	 */
	public StringBuilder addMUStatements(){
		StringBuilder muStatement = new StringBuilder();
		for(Integer thetaOrder : thetasToEtaOrder.keySet()){
			muStatement.append(Formatter.endline(MU+thetaOrder+" = "+LOG+"("+thetasToEtaOrder.get(thetaOrder)+")" ));
		}
		
		return muStatement;
	}
	
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
				OmegaStatements.put(symbId, omegaStatement);
			}
		}
	}
	
	/**
	 * Add SD attribute to sigma statement if value is obtained from standard deviation.
	 * @param statement
	 * @param isStdDev
	 */
	public void addAttributeForStdDev(StringBuilder statement, Boolean isStdDev) {
		if(isStdDev){
			statement.append(Formatter.endline(" "+SD));
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
	 * Checks the order of Etas and use this order while arranging Omega blocks.
	 * This method will create map for EtaOrder which will be order for respective Omegas as well.
	 * @return 
	 */
	public LinkedHashMap<String, Integer> createOrderedEtasMap(){
		LinkedHashMap<String, Integer> etasOrderMap = new LinkedHashMap<String, Integer>();
		//We need to have this as list as this will retains order of etas
		List<String> etasOrder = getAllEtasList();
		
		if(!etasOrder.isEmpty()){
			Integer etaCount = 0;
			Map<String, String> etaTocorrelationsMap = addCorrelationValuesToMap(getAllCorrelations());
			
			List<String> nonOmegaBlockEtas = new ArrayList<String>();
			//order etas map
			for(String eta : etasOrder) {
				//no correlations so no Omega block
				if(!etaTocorrelationsMap.isEmpty() && etaTocorrelationsMap.keySet().contains(eta)){
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
	private List<String> getAllEtasList() {
		List<String> etasOrder = new ArrayList<String>();
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		
		for(ParameterBlock block : blocks ){
			for(IndividualParameterType parameterType: block.getIndividualParameters()){
				if (parameterType.getGaussianModel() != null) {
		    		List<ParameterRandomEffectType> randomEffects = parameterType.getGaussianModel().getRandomEffects();
					for (ParameterRandomEffectType randomEffect : randomEffects) {
						if (randomEffect == null) continue;
						String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
						etasOrder.add(eta);
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
	public void createOrderedThetasToEtaMap(){
		LinkedHashMap<String, Integer> etasOrderMap = createOrderedEtasMap();
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
	private void addToThetasOrderMap(LinkedHashMap<String, Integer> etasOrderMap, Integer nextEtaOrder) {
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
				return lcov.getPopulationParameter().getAssign().getEquation().getSymbRef().getSymbIdRef();		
		}else{
			return new String();
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
	
	public void addCorrelationToMap(LinkedHashMap<String, String> etaTocorrelationsMap, Correlation correlation) {
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
	
	/**
	 * This method will reverse the map and return a tree map (ordered in natural order of keys).
	 * 
	 * @param map
	 * @return
	 */
	public <K,V> TreeMap<V,K> reverseMap(Map<K,V> map) {
		TreeMap<V,K> rev = new TreeMap<V, K>();
	    for(Map.Entry<K,V> entry : map.entrySet())
	        rev.put(entry.getValue(), entry.getKey());
	    return rev;
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
		return OmegaStatements;
	}
	
	public Map<String, SimpleParameterType> getSimpleParams() {
		return simpleParams;
	}
	
	public OmegaBlockStatement getOmegaBlockStatement() {
		return omegaBlockStatement;
	}

	public ParametersHelper(ScriptDefinition scriptDefinition){
		this.scriptDefinition = scriptDefinition;		
	}
}
