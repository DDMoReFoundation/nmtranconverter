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
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;

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
