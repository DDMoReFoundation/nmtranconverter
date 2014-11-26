package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseRandomVariableBlock.Correlation;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameterType;
import eu.ddmore.libpharmml.dom.modellingsteps.InitialEstimateType;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimateType;
import eu.ddmore.libpharmml.dom.uncertml.AbstractContinuousUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

public class ParametersHelper {
	
	ScriptDefinition scriptDefinition;
	List<SimpleParameterType> simpleParameterTypes = new ArrayList<SimpleParameterType>();
	final Map<String, ThetaStatement> thetaStatements = new HashMap<String, ThetaStatement>();
	final Map<String, OmegaStatement> OmegaStatements = new HashMap<String, OmegaStatement>();
	
	final Map<String, List<OmegaStatement>> OmegaBlocks = new HashMap<String, List<OmegaStatement>>();
	String omegaBlockTitle;

//	Map<String, String> randomVarsInCorrelation = new HashMap<String>();
	Map<String, String> etasToOmegasInCorrelation = new HashMap<String, String>();
	
	// These are keyed by symbol ID
	final Map<String, SimpleParameterType> simpleParams = new HashMap<String, SimpleParameterType>();
	Map<String, Integer> etaToOmagaMap = new HashMap<String, Integer>();
	Map<Integer, String> orderedEtasMap;
	List<ParameterEstimateType> parametersToEstimate = new ArrayList<ParameterEstimateType>();
	List<FixedParameter> fixedParameters = new ArrayList<FixedParameter>();
	final Map<String, InitialEstimateType> initialEstimates = new HashMap<String, InitialEstimateType>();
	final Map<String, ScalarRhs> lowerBounds = new HashMap<String, ScalarRhs>();
	final Map<String, ScalarRhs> upperBounds = new HashMap<String, ScalarRhs>();
	
	public void getParameters(List<SimpleParameterType> simpleParameterTypes){
				
		if (simpleParameterTypes.isEmpty()) {
			return;
		}else {
			setSimpleParameterTypes(simpleParameterTypes);
		}
		
		setEtaToOmagaMap(setEtaToOmegaOrder());
		final EstimationStep estimationStep = getEstimationStep(scriptDefinition);
		parametersToEstimate = (estimationStep.hasParametersToEstimate())? estimationStep.getParametersToEstimate():new ArrayList<ParameterEstimateType>();
		fixedParameters = (estimationStep.hasFixedParameters())? estimationStep.getFixedParameters():new ArrayList<FixedParameter>();
		// Find any bounds and initial estimates
		setAllParameterBounds(parametersToEstimate);
		
		//need to set omegas before setting theta params
		//setOmegaBlocks before omega params and theta
		createOmegaBlocks();
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

		for(ParameterEstimateType parameter : parametersToEstimate){
			String paramName = parameter.getSymbRef().getSymbIdRef();
			if(validateParamName(paramName)){
				ThetaStatement thetaStatement = new ThetaStatement(paramName);
				thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
				thetaStatements.put(paramName, thetaStatement);					
			}
		}
		for(FixedParameter fixedParameter : fixedParameters){
			String paramName = fixedParameter.p.getSymbRef().getSymbIdRef();
			if(validateParamName(paramName)){
				ThetaStatement thetaStatement = new ThetaStatement(paramName);
				thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
				thetaStatement.setFixed(true);
				thetaStatements.put(paramName, thetaStatement);					
			}
		}
	}

	private boolean validateParamName(String paramName) {
		return !(paramName== null ||  OmegaStatements.containsKey(paramName) || etasToOmegasInCorrelation.values().contains(paramName) || 
				paramName.startsWith("sigma") || thetaStatements.containsKey(paramName));
	}
	
	/**
	 * This method will create omega blocks if there are any.
	 * We will need ordered etas and eta to omega map to determine order of the omega block elements.
	 * Currently only correlations are supported.
	 *  
	 * @return
	 */
	public void createOmegaBlocks(){
		List<Correlation> correlations = getAllCorrelations();
		
		if(!correlations.isEmpty()){
			initialiseOmegaBlocks(correlations);
			orderedEtasMap = reverseMap(etaToOmagaMap);
			omegaBlockTitle = createOmegaBlockTitle(correlations);

			for(String eta : orderedEtasMap.values()){

				for(Correlation correlation :  correlations){
					String randomVar1 = correlation.rnd1.getSymbId();
					String randomVar2 = correlation.rnd2.getSymbId();
					int column = getOrderedEtaIndex(randomVar1);
					int row = getOrderedEtaIndex(randomVar2);

					createFirstMatrixRow(eta, correlation.rnd1);
						
					List<OmegaStatement> omegas = OmegaBlocks.get(randomVar2);
					if(omegas.get(row)==null){
						omegas.remove(row);
						String symbId = getNameFromParamRandomVariable(correlation.rnd2);
						omegas.add(row, getOmegaFromRandomVariable(symbId));
					}
					
					if(omegas.get(column)==null){
						String symbId = correlation.correlationCoefficient.getSymbRef().getSymbIdRef();
						omegas.remove(column);
						omegas.add(column, getOmegaFromRandomVariable(symbId));	
					}
				}
			}
		}
	}

	/**
	 * Creates title for omega blocks.
	 * Currently it gets block count for BLOCK(n) and identify in correlations are used.
	 * This will be updated for matrix types in future.
	 * 
	 * @param correlations
	 * @return
	 */
	private String createOmegaBlockTitle(List<Correlation> correlations) {
		Integer blocksCount = etaToOmagaMap.size();
		//This will change in case of 0.4 as it will need to deal with matrix types as well.
		String description = (!correlations.isEmpty())?"CORRELATION":"";
		String title = "\n$OMEGA BLOCK("+blocksCount+") "+description;
		return title;
	}

	/**
	 * This method will return index from ordered eta map for random var name provided. 
	 * 
	 * @param randomVariable
	 * @return
	 */
	private int getOrderedEtaIndex(String randomVariable) {
		return (etaToOmagaMap.get(randomVariable)>0)?etaToOmagaMap.get(randomVariable)-1:0;
	}
	
	/**
	 * This method will reverse the map and return a tree map (ordered in natural order of keys).
	 * 
	 * @param map
	 * @return
	 */
	private <K,V> TreeMap<V,K> reverseMap(Map<K,V> map) {
		TreeMap<V,K> rev = new TreeMap<V, K>();
	    for(Map.Entry<K,V> entry : map.entrySet())
	        rev.put(entry.getValue(), entry.getKey());
	    return rev;
	}

	/**
	 * Collects correlations from all the prameter blocks. 
	 * 
	 * @return
	 */
	private List<Correlation> getAllCorrelations() {
		List<Correlation> correlations = new ArrayList<Correlation>();
		List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
		if(!parameterBlocks.isEmpty()){
			for(ParameterBlock block : parameterBlocks){
				correlations.addAll(block.getCorrelations());				
			}
		}
		return correlations;
	}

	/**
	 * Creates first matrix row which will have only first omega as element.
	 * 
	 * @param eta
	 * @param randomVar1
	 */
	private void createFirstMatrixRow(String eta, ParameterRandomVariableType randomVar1) {
		if(etaToOmagaMap.get(randomVar1.getSymbId())== 1 && eta.equals(randomVar1.getSymbId())){
			List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
			String symbId = getNameFromParamRandomVariable(randomVar1);
			matrixRow.add(getOmegaFromRandomVariable(symbId));
			OmegaBlocks.put(randomVar1.getSymbId(), matrixRow);
		}
	}

	/**
	 * Get omega object from random variable provided.
	 * This will set omega symb id as well as bounds if there are any.
	 * 
	 * @param randomVar
	 * @return
	 */
	public OmegaStatement getOmegaFromRandomVariable(String omegaSymbId) {
		OmegaStatement omegaStatement = null;
		if(omegaSymbId!= null){
			omegaStatement = new OmegaStatement(omegaSymbId);
			omegaStatement.setParameterBounds(initialEstimates.get(omegaSymbId),lowerBounds.get(omegaSymbId),upperBounds.get(omegaSymbId));
		}
		return omegaStatement;
	}
	
	/**
	 * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
	 * 
	 * @param correlations
	 */
	public void initialiseOmegaBlocks(List<Correlation> correlations){
		OmegaBlocks.clear();
		//create correlations map
		for(Correlation correlation : correlations){
			etasToOmegasInCorrelation.put(correlation.rnd1.getSymbId(),getNameFromParamRandomVariable(correlation.rnd1));
			etasToOmegasInCorrelation.put(correlation.rnd2.getSymbId(),getNameFromParamRandomVariable(correlation.rnd2));
			etasToOmegasInCorrelation.put(correlation.correlationCoefficient.getSymbRef().getSymbIdRef(), 
					correlation.correlationCoefficient.getSymbRef().getSymbIdRef());
		}
		
		for(Iterator<Entry<String, Integer>> it = etaToOmagaMap.entrySet().iterator(); it.hasNext();) {
		      Entry<String, Integer> entry = it.next();
		      if(!etasToOmegasInCorrelation.keySet().contains(entry.getKey())){
		    	  it.remove();
		      }
		}

		for(String eta : etaToOmagaMap.keySet()){
			ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
			for(int i=0;i<etaToOmagaMap.keySet().size();i++) statements.add(null);
			OmegaBlocks.put(eta, statements);
		}
	}
	
	/**
	 * This method will set omega parameters apart from omega blocks.
	 * 
	 */
	private void setOmegaParameters(){
		List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
		//Unless parameterBlocks is empty, getting first parameter Block.
		createOmegaBlocks();
		if(!parameterBlocks.isEmpty()){
			for (ParameterRandomVariableType rv : parameterBlocks.get(0).getRandomVariables()) {
				String symbId = getNameFromParamRandomVariable(rv);
					OmegaStatement omegaStatement = getOmegaFromRandomVariable(symbId);
					if(omegaStatement!=null)
						OmegaStatements.put(symbId, omegaStatement);					
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
	public Map<String, Integer> setEtaToOmegaOrder(){
		Map<String, Integer> etasOrder = new HashMap<String, Integer>();
		Integer etaOrder = 0;
		
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		for(ParameterBlock block : blocks ){
			for(IndividualParameterType parameterType: block.getIndividualParameters()){
				if (parameterType.getGaussianModel() != null) {
		    		List<ParameterRandomEffectType> randomEffects = parameterType.getGaussianModel().getRandomEffects();
					if (!randomEffects.isEmpty()) {
						for (ParameterRandomEffectType random_effect : randomEffects) {
							if (random_effect == null) continue;
							etasOrder.put(random_effect.getSymbRef().get(0).getSymbIdRef(), ++etaOrder);
						}
					}
				}
			}
		}
		return etasOrder;
	}

	//Getters and setters

	public ScriptDefinition getScriptDefinition() {
		return scriptDefinition;
	}

	public void setScriptDefinition(ScriptDefinition scriptDefinition) {
		this.scriptDefinition = scriptDefinition;
	}

	public List<SimpleParameterType> getSimpleParameterTypes() {
		return simpleParameterTypes;
	}

	public void setSimpleParameterTypes(List<SimpleParameterType> simpleParameterTypes) {
		this.simpleParameterTypes = simpleParameterTypes;
	}

	public ParametersHelper(ScriptDefinition scriptDefinition){
		this.scriptDefinition = scriptDefinition;		
	}
	
	public List<ParameterEstimateType> getParametersToEstimate() {
		return parametersToEstimate;
	}
	
	public Map<String, ThetaStatement> getThetaParams() {
		return thetaStatements;
	}

	public Map<String, OmegaStatement> getOmegaParams() {
		return OmegaStatements;
	}
	
	public Map<String, InitialEstimateType> getInitialEstimates() {
		return initialEstimates;
	}

	public Map<String, ScalarRhs> getLowerBounds() {
		return lowerBounds;
	}

	public Map<String, ScalarRhs> getUpperBounds() {
		return upperBounds;
	}
	
	public Map<String, SimpleParameterType> getSimpleParams() {
		return simpleParams;
	}
	
	public Map<String, Integer> setEtaToOmagaMap() {
		return etaToOmagaMap;
	}
	
	public Map<Integer, String> getOrderedEtasMap() {
		return orderedEtasMap;
	}

	public void setEtaToOmagaMap(Map<String, Integer> etaToOmagaMap) {
		this.etaToOmagaMap = etaToOmagaMap;
	}
	
	public Map<String, List<OmegaStatement>> getOmegaBlocks() {
		return OmegaBlocks;
	}
	
	public String getOmegaBlockTitle() {
		return omegaBlockTitle;
	}

}
