package eu.ddmore.converters.nonmem.statements;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.Parser;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.InitialValueType;
import eu.ddmore.libpharmml.dom.commontypes.IntValueType;
import eu.ddmore.libpharmml.dom.commontypes.RealValueType;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinitionType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ErrorModel;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationErrorType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class PredStatement {
	
	final String DES_VAR_SUFFIX = "_DES";
	ScriptDefinition scriptDefinition;
	List<DerivativeVariableType> derivativeVarList = new ArrayList<DerivativeVariableType>();
	Map<String, String> derivativeVariableMap = new HashMap<String, String>();
	//it will hold definition types and its parsed equations which we will need to add in Error statement.
	Map<String, String> definitionsParsingMap = new HashMap<String, String>();
	List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();
	public static Boolean isDES = false;
	Parser parser;


	public PredStatement(ScriptDefinition scriptDefinition, Parser parser){
		this.parser = parser;
		this.scriptDefinition = scriptDefinition;
		derivativeVarList.addAll(getAllStateVariables());
	}
	
	public void getPredStatement(PrintWriter fout){
		String statementName = Formatter.endline()+Formatter.endline("$PRED");
		if(!derivativeVarList.isEmpty()){
			//TODO: Add $SUB block. need to have details around it.
			statementName = Formatter.endline()+Formatter.endline("$SUB");
			fout.write(Formatter.endline()+Formatter.endline("$SUBS ADVAN13 TOL=9"));
			fout.write(getDerivativePredStatement().toString());
		}else{
			fout.write(statementName);
			fout.write(getNonDerivativePredStatement().toString());
		}
	}

	/**
	 * Returns non derivative pred statement.
	 * @return 
	 * 
	 */
	private StringBuilder getNonDerivativePredStatement() {
        StringBuilder sb = new StringBuilder();
        //NM_D is for DOSE
        sb.append(Formatter.endline());
        sb.append(Formatter.endline("IF (AMT.GT.0) NM_D=AMT"));
        sb.append(getPredCoreStatement());
        sb.append(getErrorStatement());
        
        return sb;
	}	
	
	/**
	 * This method will build theta assignment statements
	 * @param fout
	 */
	public StringBuilder buildThetaAssignments() {
		StringBuilder thetaAssignmentBlock = new StringBuilder();  
    	for(String theta : parser.getParameters().getThetaParams().keySet()){
    		thetaAssignmentBlock.append(Formatter.endline(theta+ " = "+parser.getThetaForSymbol(theta)));
    	}
    	return thetaAssignmentBlock;
	}
	
	/**
	 * This method will build eta assignment statements to be displayed after theta assignments.
	 * @return
	 */
	public StringBuilder buildEtaAssignments() {
		StringBuilder etaAssignment = new StringBuilder();  
    	for(String eta : parser.getEtasOrder().keySet()){
    		etaAssignment.append(Formatter.endline(eta+ " = ETA("+parser.getEtasOrder().get(eta)+")"));
    	}
    	return etaAssignment;
	}

	/**
	 * gets pred core statement for nonmem file.
	 */
	private StringBuilder getPredCoreStatement() {
		StringBuilder predCoreBlock = new StringBuilder();
		List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
		predCoreBlock.append(Formatter.endline(buildThetaAssignments().toString()));
		predCoreBlock.append(Formatter.endline(buildEtaAssignments().toString()));
		predCoreBlock.append(getAllIndividualParamAssignments(blocks));
		return predCoreBlock;
	}

	private StringBuilder getDerivativePredStatement() {
		StringBuilder DerivativePredblock = new StringBuilder();
		DerivativePredblock.append(getModelStatement());
		//TODO : getAbbreviatedStatement();
		DerivativePredblock.append(getPKStatement());
		errorStatements = prepareAllErrorStatements();
		isDES= true;
		DerivativePredblock.append(getDifferentialEquationsStatement());
		isDES = false;
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
		diffEqStatementBlock.append(Formatter.endline("$DES"));
		int i=1;
		for (DerivativeVariableType variableType : derivativeVarList){
			String variable = Formatter.addPrefix(variableType.getSymbId());
			derivativeVariableMap.put(variable, Integer.toString(i++));
			
			String varAmount = getVarAmountFromCompartment(variable, derivativeVariableMap);
			if(!varAmount.isEmpty())
				diffEqStatementBlock.append(Formatter.endline(variable+" = "+varAmount));
			if(isVarFromErrorFunction(variable))
				definitionsParsingMap.put(variable, varAmount);
		}
		diffEqStatementBlock.append(Formatter.endline());
		for(StructuralBlock block : scriptDefinition.getStructuralBlocks()){
			diffEqStatementBlock.append(addVarDefinitionTypesToDES(block));
			diffEqStatementBlock.append(addDerivativeVarToDES(block));
		}
		return diffEqStatementBlock;
	}

	/**
	 * This method gets variable amount from compartment and returns it.
	 * 
	 * @param variable
	 * @return
	 */
	public static String getVarAmountFromCompartment(String variable, Map<String,String> derivativeVariableMap) {
		String varAmount = new String(); 
		varAmount = derivativeVariableMap.get(variable);
		if(!varAmount.isEmpty()){
			varAmount = "A("+varAmount+")";
		}
		return varAmount;
	}
	
	/**
	 * This method gets variable definitions for the variables and adds them to DES
	 * As workaround to issues with variables used in Error model, we rename those variables in DES block
	 *   
	 * @param block
	 * @return
	 */
	private StringBuilder addVarDefinitionTypesToDES(StructuralBlock block) {
		StringBuilder varDefinitionsBlock = new StringBuilder();
		for (VariableDefinitionType definitionType: block.getLocalVariables()){
			String variable = Formatter.addPrefix(definitionType.getSymbId());
			String rhs = parser.parse(definitionType).replaceFirst(variable+" =","");			
			if(isVarFromErrorFunction(variable)){
				definitionsParsingMap.put(variable, rhs);
				variable = renameFunctionVariableForDES(variable);
			}
			varDefinitionsBlock.append(variable+" = "+rhs);
		}
		return varDefinitionsBlock;
	}

	/**
	 * This method will parse DADT variables from derivative variable type definitions 
	 * and adds it to DES block.
	 * 
	 * @param block
	 * @return
	 */
	private StringBuilder addDerivativeVarToDES(StructuralBlock block) {
		StringBuilder derivativeVarBlock = new StringBuilder();
		for(DerivativeVariableType variableType: block.getStateVariables()){
			String parsedDADT = parser.parse(variableType);
			String variable = Formatter.addPrefix(variableType.getSymbId());
			
			if(derivativeVariableMap.containsKey(variable)){
				String index = derivativeVariableMap.get(variable);
				parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
			}
			for(String derivativeVar : definitionsParsingMap.keySet()){
				String varToReplace = new String("\\b"+Pattern.quote(derivativeVar)+"\\b");
				parsedDADT = parsedDADT.replaceAll(varToReplace, renameFunctionVariableForDES(derivativeVar));
			}
			derivativeVarBlock.append(parsedDADT);
		}
		return derivativeVarBlock;
	}

	/**
	 * Determines if variable is function variable from error model.
	 * 
	 * @param variable
	 * @return
	 */
	private Boolean isVarFromErrorFunction(String variable){
		for(ErrorStatement errorStatement : errorStatements){
			if(errorStatement.getFunction().equals(variable)){
				return true;
			}
		}
		return false;
	}

	/**
	 * This method will rename variable which is defined as Function variable in error model block.
	 * This will be used in DES statement.
	 * @param variable
	 * @return
	 */
	private String renameFunctionVariableForDES(String variable) {
		variable = variable+DES_VAR_SUFFIX;
		return variable; 
	}
	
	/**
	 * get Error statement for nonmem pred block
	 * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
	 * @return 
	 * 
	 */
	private String getErrorStatement() {
		StringBuilder errorBlock = new StringBuilder();
		errorBlock.append(Formatter.endline());
		errorBlock.append(Formatter.endline("$ERROR"));
		//definitionsParsingMap and derivativeVariableMap are set up as part of DES before this step.
		for(ErrorStatement errorStatement: errorStatements){
			errorBlock.append(errorStatement.getErrorStatementDetails(definitionsParsingMap,derivativeVariableMap));
		}
		return errorBlock.toString();
	}
	
	/**
	 * This method will list prepare all the error statements and returns the list.
	 * We need to prepare this list separately as we need to use it in DES block before writing out to ERROR block.
	 * @return
	 */
	private List<ErrorStatement> prepareAllErrorStatements(){
		List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();

		for(ObservationBlock block : scriptDefinition.getObservationBlocks()){
			ObservationErrorType errorType = block.getObservationError();
			if(errorType instanceof GaussianObsError){
				GaussianObsError error = (GaussianObsError) errorType;
				ErrorModel errorModel = error.getErrorModel();
				String output = error.getOutput().getSymbRef().getSymbIdRef();
				FunctionCallType functionCall = errorModel.getAssign().getEquation().getFunctionCall();
				
				ErrorStatement errorStatement = new ErrorStatement(functionCall, output);
				errorStatements.add(errorStatement);
			}else if(errorType instanceof GeneralObsError){
//				GeneralObsError genError = (GeneralObsError) errorType;
//				TODO : DDMORE-1013 : add support for general observation error type once details are available 
			}else{
//				TODO : Check if there are any other types to encounter
			}
		}
		return errorStatements;
	}

	/**
	 * gets AES block for pred statement of nonmem file. 
	 */
	private void getAESStatement() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * gets pk block for pred statement
	 */
	private StringBuilder getPKStatement() {
		StringBuilder pkStatementBlock = new StringBuilder();
		pkStatementBlock.append(Formatter.endline());
		pkStatementBlock.append(Formatter.endline("$PK"));
		pkStatementBlock.append(getPredCoreStatement());
		pkStatementBlock.append(getDifferentialInitialConditions());
		return pkStatementBlock;
	}

	/**
	 * gets ABBR block for pred statement of nonmem file.
	 * TODO Currently ABBR block is not in scope.
	 */
//	private StringBuilder getAbbreviatedStatement() {
//		return null;
//	}

	/**
	 * get model statement block for pred statement of nonmem file.
	 * 
	 */
	private StringBuilder getModelStatement() {
		StringBuilder modelBlock = new StringBuilder();
		modelBlock.append(Formatter.endline());
		modelBlock.append(Formatter.endline("$MODEL"));
		for(DerivativeVariableType stateVariable :getAllStateVariables()){
			modelBlock.append(Formatter.endline("COMP "+"("+Formatter.addPrefix(stateVariable.getSymbId())+")"));
		}
		return modelBlock;
	}
	
	/**
	 * Collects all derivativeVariable types (state variables) from structural blocks in order to create model statement.
	 * 
	 * @return
	 */
	private Set<DerivativeVariableType> getAllStateVariables() {
		Set<DerivativeVariableType> stateVariables = new LinkedHashSet<DerivativeVariableType>();
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
							builder.append(Formatter.endline("A_0("+i+") = "+Formatter.addPrefix(initialCondition.toUpperCase()))); 
						}else if(initialValueType.getAssign().getScalar()!=null){
							Object value =  initialValueType.getAssign().getScalar().getValue();
							
							if(value instanceof RealValueType){
								initialCondition = Double.toString(((RealValueType)value).getValue());
							} else if(value instanceof IntValueType){
								initialCondition = ((IntValueType)value).getValue().toString();
							}
							
							builder.append(Formatter.endline("A_0("+i+") = "+initialCondition));
						}
					}
				}
				i++;
			}
		}
		return builder;
	}
	
	/**
	 * This method will collect all the parsing for Individual parameter assignments.
	 *  
	 * @param blocks
	 * @return
	 */
	public StringBuilder getAllIndividualParamAssignments(List<ParameterBlock> blocks) {
		StringBuilder IndividualParamAssignmentBlock = new StringBuilder();
		for(ParameterBlock parameterBlock : blocks){
			for(IndividualParameterType parameterType: parameterBlock.getIndividualParameters()){
				IndividualParamAssignmentBlock.append(parser.createIndividualDefinition(parameterType));	
			}
		}
		return IndividualParamAssignmentBlock;
	}
}
