package ddmore.converters.nonmem.statements;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import ddmore.converters.nonmem.Parser;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.InitialValueType;
import eu.ddmore.libpharmml.dom.commontypes.RealValueType;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinitionType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationErrorType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ErrorModel;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class PredStatement {
	
	ScriptDefinition scriptDefinition;
	List<DerivativeVariableType> derivativeVarList = new ArrayList<DerivativeVariableType>();
	Map<String, String> derivativeVariableMap = new HashMap<String, String>();
	Parser parser;


	public PredStatement(ScriptDefinition scriptDefinition, Parser parser){
		this.parser = parser;
		this.scriptDefinition = scriptDefinition;
		derivativeVarList.addAll(getAllStateVariables());
	}
	
	public void getPredStatement(PrintWriter fout){
		String statementName = "\n$PRED\n";
		if(!derivativeVarList.isEmpty()){
			//TODO: Add $SUB block. need to have details around it.
			statementName = "\n$SUB\n";
			fout.write("\n$SUBS ADVAN13 TOL=9\n");
			fout.write(getDerivativePredStatement().toString());
		}else{
			fout.write(statementName);
			getNonDerivativePredStatement();
		}
	}

	/**
	 * Returns non derivative pred statement.
	 * 
	 */
	private void getNonDerivativePredStatement() {
        StringBuilder sb = new StringBuilder();
        //NM_D is for DOSE
        sb.append("\nIF (AMT.GT.0) NM_D=AMT\n");
        sb.append(getPredCoreStatement());
        sb.append(getErrorStatement());
	}
	
	/**
	 * This method will build theta assignment statements
	 * @param fout
	 */
	public StringBuilder buildThetaAssignments() {
		StringBuilder thetaAssignmentBlock = new StringBuilder();  
    	for(String theta : parser.getParameters().getThetaParams().keySet()){
    		thetaAssignmentBlock.append("\n"+theta+ " = "+parser.getThetaForSymbol(theta)+"\n");
    	}
    	return thetaAssignmentBlock;
	}

	/**
	 * gets pred core statement for nonmem file.
	 */
	private StringBuilder getPredCoreStatement() {
		StringBuilder predCoreBlock = new StringBuilder();
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		predCoreBlock.append(buildThetaAssignments().toString());
		
		for(ParameterBlock parameterBlock : blocks){
			for(IndividualParameterType parameterType: parameterBlock.getIndividualParameters()){
				predCoreBlock.append(doIndividualParameterAssignment(parameterType));	
			}
		}
		return predCoreBlock;
	}

	private StringBuilder getDerivativePredStatement() {
		StringBuilder DerivativePredblock = new StringBuilder();
		DerivativePredblock.append(getModelStatement());
		//TODO : getAbbreviatedStatement();
		DerivativePredblock.append(getPKStatement());
		DerivativePredblock.append(getDifferentialEquationsStatement());
		getAESStatement();
		DerivativePredblock.append(getErrorStatement());
        
        return DerivativePredblock;
	}

	/**
	 * gets DES block for pred statement
	 * 
	 */
	private StringBuilder getDifferentialEquationsStatement() {
		StringBuilder diffEqStatementBlock = new StringBuilder();
		diffEqStatementBlock.append("\n$DES\n");
		int i=1;
		for (DerivativeVariableType variableType : derivativeVarList){
			String variable = "NM_"+variableType.getSymbId().toUpperCase();
			diffEqStatementBlock.append("NM_"+variableType.getSymbId().toUpperCase()+" = A("+i+")\n");
			derivativeVariableMap.put(variable, Integer.toString(i));
			i++;
		}
		diffEqStatementBlock.append("\n");
		for(StructuralBlock block : scriptDefinition.getStructuralBlocks()){
			for (VariableDefinitionType definitionType: block.getLocalVariables()){
				diffEqStatementBlock.append(parser.parse(definitionType));
			}
			diffEqStatementBlock.append("\n");
			for(DerivativeVariableType variableType: block.getStateVariables()){
				String parsedDADT = parser.parse(variableType);
				String variable = "NM_"+variableType.getSymbId().toUpperCase();
				if(derivativeVariableMap.keySet().contains(variable)){
					String index = derivativeVariableMap.get(variable);
					//TODO: String formatting can go as part of formatter class. 
					parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
				}
				diffEqStatementBlock.append(parsedDADT);
			}
		}
		return diffEqStatementBlock;

	}
	
	/**
	 * get Error statement for nonmem pred block
	 * @return 
	 * 
	 */
	private String getErrorStatement() {
		StringBuilder errorBlock = new StringBuilder();
		errorBlock.append("\n$ERROR\n");
		
		List<ObservationBlock> observationBlocks= scriptDefinition.getObservationBlocks();
		for(ObservationBlock block : observationBlocks){
			GaussianObsError error = (GaussianObsError) block.getObservationError();
			ErrorModel errorModel = error.getErrorModel();
			FunctionCallType functionCall = errorModel.getAssign().getEquation().getFunctionCall();
			
			ErrorStatement errorstmt = new ErrorStatement(functionCall);			
			errorBlock.append(errorstmt.getErrorStatementDetails());

		}
		return errorBlock.toString();
	}

	/**
	 * 
	 */
	private void getAESStatement() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * gets pk block for pred statement
	 */
	private StringBuilder getPKStatement() {
		StringBuilder pkStatementBlock = new StringBuilder();
		pkStatementBlock.append("\n$PK\n");
		pkStatementBlock.append(getPredCoreStatement());
		pkStatementBlock.append(getDifferentialInitialConditions());
		return pkStatementBlock;
	}

	/**
	 * gets ABBR block for pred statement of nonmem file.
	 */
	private StringBuilder getAbbreviatedStatement() {
		return null;
		// TODO Auto-generated method stub
		
	}

	/**
	 * get model statement block for pred statement of nonmem file.
	 * 
	 */
	private StringBuilder getModelStatement() {
		StringBuilder modelBlock = new StringBuilder();
		
		modelBlock.append("\n$MODEL\n");
		for(DerivativeVariableType stateVariable :getAllStateVariables()){
			modelBlock.append("COMP "+"("+"NM_"+stateVariable.getSymbId()+")\n");
		}
		return modelBlock;
	}
	
	/**
	 * Collects all derivativeVariable types (state variables) from structural blocks in order to create model statement.
	 * 
	 * @return
	 */
	private Set<DerivativeVariableType> getAllStateVariables() {
		Set<DerivativeVariableType> stateVariables = new HashSet<DerivativeVariableType>();
		for(StructuralBlock structuralBlock : scriptDefinition.getStructuralBlocks() ){
			stateVariables.addAll(structuralBlock.getStateVariables());
		}
		return stateVariables;
	}
	
	/**
	 * Creates DES statement block from differential initial conditions.
	 * 
	 * @return
	 */
	public StringBuilder getDifferentialInitialConditions(){
		
		StringBuilder builder = new StringBuilder();
		
		for(StructuralBlock structBlock : scriptDefinition.getStructuralBlocks()){
			int i = 1;
			for(DerivativeVariableType variableType : structBlock.getStateVariables()){
				String initialCondition = new String();
				if(variableType.getInitialCondition()!=null){
					InitialValueType initialValueType = variableType.getInitialCondition().getInitialValue();
					if(initialValueType!=null){
						if(initialValueType.getAssign().getSymbRef()!=null){
							initialCondition = initialValueType.getAssign().getSymbRef().getSymbIdRef();
							builder.append("A_0("+i+") = NM_"+initialCondition.toUpperCase()+"\n"); 
						}else if(initialValueType.getAssign().getScalar()!=null){
							RealValueType test = (RealValueType) initialValueType.getAssign().getScalar().getValue();
							initialCondition = Double.toString(test.getValue());
							builder.append("A_0("+i+") = "+initialCondition+"\n");
						}
					}
				}
				i++;
			}
		}
		return builder;
	}
	
	/**
	 * This method will create part of pred core statement block from individual parameter assignments.
	 * Currently it refers to  
	 * @param ip
	 * @return
	 */
	public String doIndividualParameterAssignment(IndividualParameterType ip) {
		return parser.doIndividualParameterAssignment(ip);
	}
}
