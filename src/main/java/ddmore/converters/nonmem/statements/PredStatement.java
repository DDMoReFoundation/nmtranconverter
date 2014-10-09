package ddmore.converters.nonmem.statements;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.CovariateBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariateType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinitionType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel.LinearCovariate.PopulationParameter;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class PredStatement {
	
	ScriptDefinition scriptDefinition;
	List<DerivativeVariableType> derivativeVarList = new ArrayList<DerivativeVariableType>();


	public PredStatement(ScriptDefinition scriptDefinition){
		this.scriptDefinition = scriptDefinition;
		List<StructuralBlock> structuralBlocks = scriptDefinition.getStructuralBlocks();
		for(StructuralBlock block : structuralBlocks){
			 derivativeVarList.addAll(block.getStateVariables());
		}
	}
	
	public void getPredStatement(PrintWriter fout){
//		StringBuilder predStatementblock = new StringBuilder();
		
		if(!derivativeVarList.isEmpty()){
			fout.write(getDerivativePredStatement().toString());
		}else{
			getNonDerivativePredStatement();
		}
	}

	private void getNonDerivativePredStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("IF (AMT.GT.0) ${rename('DOSE')}=AMT");
        getPredCoreStatement();
        getErrorStatement();
		
	}

	/**
	 * gets pred core statement for nonmem file.
	 */
	private void getPredCoreStatement() {
		/*
		 *         sb << getCovariatesFromModel()
        sb << getIndividualsFromModel()
        if (!getDerivativeVariableTypes()) {
            sb << getStructuralParameters()
        }
        sb << getConditionalStatements().join("")
		 */
//		getCovariatesFromModel();
	}

	private StringBuilder getCovariatesFromModel() {
		
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		for(ParameterBlock parameterBlock : blocks){
			List<IndividualParameterType> individualParams = parameterBlock.getIndividualParameters();
			
			for(IndividualParameterType individualParam: individualParams ){
				String symbid = individualParam.getSymbId();
				
				if(individualParam.getGaussianModel() != null){
					if(individualParam.getGaussianModel().getLinearCovariate() != null){
						CovariateRelationType relationType = individualParam.getGaussianModel().getLinearCovariate().getCovariate().get(0);
						
					}
				}
				if(individualParam.getAssign()!=null){
					
				}
			}
		}
		
		return null;
	}
	
//	protected String doIndividualParameterAssignment(IndividualParameterType ip) {
//
//    	StringBuilder stmt = new StringBuilder();
//    	
//    	String variableSymbol = ip.getSymbId();
//    	
//    	if (ip.getAssign() != null) {
//    		stmt.append(String.format("%s = ", variableSymbol));
//    		String assignment = parse(new Object(), lexer.getStatement(ip.getAssign()));
//    		stmt.append(assignment);
//    		stmt.append(";\n");
//    	} else if (ip.getGaussianModel() != null) {
//    		String index_symbol = "ETA";
//    		GaussianModel m = ip.getGaussianModel();
//    		LhsTransformationType transform = m.getTransformation();
//    		GaussianModel.GeneralCovariate gcov = m.getGeneralCovariate();
//    		GaussianModel.LinearCovariate lcov = m.getLinearCovariate();
//    		List<ParameterRandomEffectType> random_effects = m.getRandomEffects();
//    		int nCovs = 0;
//    		
//    		if (transform == LhsTransformationType.LOG) variableSymbol = "log" + capitalise(variableSymbol);
//    		else if (transform == LhsTransformationType.LOGIT) variableSymbol = "logit" + capitalise(variableSymbol);
//    		else if (transform == LhsTransformationType.PROBIT) variableSymbol = "probit" + capitalise(variableSymbol);
//    		
//    		stmt.append(String.format("%s = ", variableSymbol));
//    		
//    		if (lcov != null) {
//    			String pop_param_symbol = null;
//    			
//    			PopulationParameter pop_param = lcov.getPopulationParameter();
//    			if (pop_param != null) {
////    				pop_param_symbol = parse(pop_param, lexer.getStatement(pop_param));
//    				
//    				if (transform == LhsTransformationType.LOG) pop_param_symbol = String.format("log(%s)", pop_param_symbol);
//    	    		else if (transform == LhsTransformationType.LOGIT) pop_param_symbol = String.format("logit(%s)", pop_param_symbol);
//    	    		else if (transform == LhsTransformationType.PROBIT) pop_param_symbol = String.format("probit(%s)", pop_param_symbol);
//    				
//    				stmt.append(String.format("(%s*ones(1, %s))", pop_param_symbol, index_symbol));
//    			} 
//    			
//    			List<CovariateRelationType> covariates = lcov.getCovariate();
//    			if (covariates != null) {
//    				if (pop_param_symbol != null) stmt.append(" + ");
//    				
//    				for (CovariateRelationType covariate : covariates) {
//    					if (covariate == null) continue;
//    					if (nCovs > 0) stmt.append(" + ");
//    					
////    					CovariateDefinitionType cdt = (CovariateDefinitionType) lexer.getAccessor().fetchElement(covariate.getSymbRef());
//    					
//    					if (cdt != null) {
//    						if (cdt.getContinuous() != null) {
//    							String cov_stmt = "";
//    							ContinuousCovariateType continuous = cdt.getContinuous();
//    							if (continuous.getTransformation() != null) cov_stmt = getSymbol(continuous.getTransformation());
//    							else cov_stmt = cdt.getSymbId();
//    							
//    							List<FixedEffectRelationType> fixed_effects = covariate.getFixedEffect();
//    							if (fixed_effects != null) {
//    								for (FixedEffectRelationType fixed_effect : fixed_effects) {
//    									if (fixed_effect == null) continue;
//    									String fixed_effect_stmt = parse(fixed_effect, lexer.getStatement(fixed_effect));
//    									cov_stmt = fixed_effect_stmt + " * " + cov_stmt;
//    									break;
//    								}
//    								
//    							}
//    							stmt.append(cov_stmt);
//    							nCovs++;
//    						} else if (cdt.getCategorical() != null) {
//    							throw new UnsupportedOperationException("No categorical yet");
//    						}
//    					}
//    				}
//    			}
//    		} else if (gcov != null) {
////    			String assignment = parse(gcov, lexer.getStatement(gcov));
////    			stmt.append(assignment);
//    			nCovs++;
//    		}
//    		
//    		int nRandomEffects = 0;
//			if (random_effects != null) {
//				if (!random_effects.isEmpty()) {
//					if (nCovs > 0) stmt.append(" + ");
//					for (ParameterRandomEffectType random_effect : random_effects) {
//						if (random_effect == null) continue;
//						if (nRandomEffects > 0) stmt.append(" + ");
////						stmt.append(parse(random_effect, lexer.getStatement(random_effect)));
//						nRandomEffects++;
//					}
//				}
//			}
//			stmt.append(";\n");
//			
//			if (transform == LhsTransformationType.LOG) {
//				String format = "%s = exp(%s);\n";
//				stmt.append(String.format(format, ip.getSymbId(), variableSymbol));
//			} else if (transform == LhsTransformationType.LOGIT) {
//				String format = "%s = 1./(1 + exp(-%s));\n";
//				stmt.append(String.format(format, ip.getSymbId(), variableSymbol));
//			} else if (transform == LhsTransformationType.PROBIT) {
//				String format = "%s = normcdf(%s);\n";
//				stmt.append(String.format(format, ip.getSymbId(), variableSymbol));
//			}
//    	}
//		stmt.append("\n");
//		
//		return stmt.toString();
//	
//		
//	}
	

	private StringBuilder getDerivativePredStatement() {
		StringBuilder DerivativePredblock = new StringBuilder();
		DerivativePredblock.append(getModelStatement());
		getAbbreviatedStatement();
		getPKStatement();
        getDifferentialEquationsStatement();
		getAESStatement();
        getErrorStatement();
        
        return DerivativePredblock;
	}

	/**
	 * get Error statement for nonmem pred block
	 * 
	 */
	private void getErrorStatement() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 */
	private void getAESStatement() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * gets DES block for pred statement
	 * 
	 */
	private void getDifferentialEquationsStatement() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * gets pk block for pred statement
	 */
	private void getPKStatement() {
		// TODO Auto-generated method stub
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		getPredCoreStatement();
		
	}

	/**
	 * gets ABBR block for pred statement of nonmem file.
	 */
	private void getAbbreviatedStatement() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * get model block for pred statement of nonmem file.
	 */
	private StringBuilder getModelStatement() {
		StringBuilder modelBlock = new StringBuilder();
		
		modelBlock.append("\n$MODEL\n");
		for(DerivativeVariableType stateVariable :getAllStateVariables()){
			modelBlock.append("COMP "+"("+"NM_"+stateVariable.getSymbId()+")\n");
		}
		return modelBlock;
	}

	private Set<DerivativeVariableType> getAllStateVariables() {
		Set<DerivativeVariableType> stateVariables = new HashSet<DerivativeVariableType>();
		for(StructuralBlock structuralBlock : scriptDefinition.getStructuralBlocks() ){
			stateVariables.addAll(structuralBlock.getStateVariables());
		}
		return stateVariables;
	}
}
