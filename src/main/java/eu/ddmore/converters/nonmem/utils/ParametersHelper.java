package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.statements.OmegaBlockStatement;
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
	
	public static final String CORRELATION = "CORRELATION";
	public static final String FIX = "FIX";
	public static final String SD = "SD";
	
	ScriptDefinition scriptDefinition;
	List<SimpleParameterType> simpleParameterTypes = new ArrayList<SimpleParameterType>();
	final Map<String, ThetaStatement> thetaStatements = new HashMap<String, ThetaStatement>();
	final Map<String, OmegaStatement> OmegaStatements = new HashMap<String, OmegaStatement>();
	
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
		omegaBlockStatement.setEtaToOmagaMap(setEtaToOmegaOrder());
		omegaBlockStatement.createOmegaBlocks();
		//need to set omegas before setting theta params
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
	 * This method will set omega parameters apart from omega blocks.
	 * 
	 */
	private void setOmegaParameters(){
		List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();

		//Unless parameterBlocks is empty, getting first parameter Block.
		if(!parameterBlocks.isEmpty()){
			for (ParameterRandomVariableType rv : parameterBlocks.get(0).getRandomVariables()) {
				String symbId = getNameFromParamRandomVariable(rv);
				OmegaStatement omegaStatement = getOmegaFromRandomVariable(symbId);
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
	
	public List<ParameterEstimateType> getParametersToEstimate() {
		return parametersToEstimate;
	}
	
	public Map<String, ThetaStatement> getThetaParams() {
		return thetaStatements;
	}

	public Map<String, OmegaStatement> getOmegaParams() {
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
