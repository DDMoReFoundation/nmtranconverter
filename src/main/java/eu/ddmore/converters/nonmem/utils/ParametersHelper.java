package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseRandomVariableBlock.Correlation;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
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
	final Map<String, ThetaStatement> thetaStatements= new HashMap<String, ThetaStatement>();
	final Map<String, OmegaStatement> OmegaStatements= new HashMap<String, OmegaStatement>();
	
	// These are keyed by symbol ID
	final Map<String, SimpleParameterType> simpleParams = new HashMap<String, SimpleParameterType>();
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
		
		final EstimationStep estimationStep = getEstimationStep(scriptDefinition);

		parametersToEstimate = (estimationStep.hasParametersToEstimate())? estimationStep.getParametersToEstimate():new ArrayList<ParameterEstimateType>();
		fixedParameters = (estimationStep.hasFixedParameters())? estimationStep.getFixedParameters():new ArrayList<FixedParameter>();
		// Find any bounds and initial estimates
		setAllParameterBounds(parametersToEstimate);
		
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
				if(!(OmegaStatements.containsKey(paramName) || paramName.startsWith("sigma") || thetaStatements.containsKey(paramName))){
					if(paramName!= null){
						ThetaStatement thetaStatement = new ThetaStatement(paramName);
						thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
						thetaStatements.put(paramName, thetaStatement);					
					}
				}
			}
		for(FixedParameter fixedParameter : fixedParameters){
			String paramName = fixedParameter.p.getSymbRef().getSymbIdRef();
			if(!(OmegaStatements.containsKey(paramName) || paramName.startsWith("sigma") || thetaStatements.containsKey(paramName))){
					if(paramName!= null){
						ThetaStatement thetaStatement = new ThetaStatement(paramName);
						thetaStatement.setParameterBounds(initialEstimates.get(paramName),lowerBounds.get(paramName),upperBounds.get(paramName));
						thetaStatement.setFixed(true);
						thetaStatements.put(paramName, thetaStatement);					
					}
					
				}
		}
	}
	
	public String setOmegaBlock(){
			
			List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
			List<Correlation> correlations = new ArrayList<Correlation>();

			if(!parameterBlocks.isEmpty()){
				for(ParameterBlock block : parameterBlocks){
					correlations.addAll(block.getCorrelations());
				}				
			}
			
			return null;
			
	}
	
	private void setOmegaParameters(){
		List<ParameterBlock> parameterBlocks = getScriptDefinition().getParameterBlocks();
		//Unless parameterBlocks is empty, getting first parameter Block.
		List<Correlation> correlations = new ArrayList<Correlation>();
		if(!parameterBlocks.isEmpty()){
			
			for(ParameterBlock block : parameterBlocks){
				correlations.addAll(block.getCorrelations());
			}
			for (ParameterRandomVariableType rv : parameterBlocks.get(0).getRandomVariables()) {
				String symbId = getNameFromParamRandomVariable(rv);
				if(symbId!= null){
					OmegaStatement omegaStatement = new OmegaStatement(symbId);
					omegaStatement.setParameterBounds(initialEstimates.get(symbId),lowerBounds.get(symbId),upperBounds.get(symbId));
					OmegaStatements.put(symbId, omegaStatement);					
				}
			}
		}
	}

	private String getNameFromParamRandomVariable(ParameterRandomVariableType rv) {
		String symbId = null;
		if (getDistributionTypeStdDev(rv) != null) {
			symbId = getDistributionTypeStdDev(rv).getVar().getVarId();					
		} else if (getDistributionTypeVariance(rv) != null) {
			symbId = getDistributionTypeVariance(rv).getVar().getVarId();
		}
		return symbId;
	}

	public PositiveRealValueType getDistributionTypeStdDev(ParameterRandomVariableType rv){
		final AbstractContinuousUnivariateDistributionType distributionType = rv.getAbstractContinuousUnivariateDistribution().getValue();
		if (distributionType instanceof NormalDistribution) {
			return ((NormalDistribution) distributionType).getStddev();
		}
		return null;
	}
	
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
}
