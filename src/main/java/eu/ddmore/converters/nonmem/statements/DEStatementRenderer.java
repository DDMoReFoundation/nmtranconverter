package eu.ddmore.converters.nonmem.statements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.Parser;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinitionType;

public class DEStatementRenderer {
	private static final String DES = "$DES";
	public String DES_VAR_SUFFIX;
	ScriptDefinition scriptDefinition;
	public Map<String, String> derivativeVariableMap = new HashMap<String, String>();
	//it will hold definition types and its parsed equations which we will need to add in Error statement as well.
	public Map<String, String> definitionsParsingMap = new HashMap<String, String>();
	List<ErrorStatement> errorStatements;
	Parser parser;

	public DEStatementRenderer(ScriptDefinition scriptDefinition, List<ErrorStatement> errorStatements, Parser parser) {
		this.scriptDefinition = scriptDefinition;
		this.errorStatements = errorStatements;
		this.parser = parser;
	}
	
	/**
	 * gets DES block for pred statement
	 * 
	 */
	public StringBuilder getDifferentialEquationsStatement(List<DerivativeVariableType> derivativeVarList) {
		StringBuilder diffEqStatementBlock = new StringBuilder();
		diffEqStatementBlock.append(Formatter.endline(DES));
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
			Boolean isDerivativeVarWithAmount = derivativeVariableMap.containsKey(variable); 
			if(isDerivativeVarWithAmount){
				String index = derivativeVariableMap.get(variable);
				parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
			}
			for(String derivativeVar : definitionsParsingMap.keySet()){
				String varToReplace = new String("\\b"+Pattern.quote(derivativeVar)+"\\b");
				if(!isDerivativeVarWithAmount){
					parsedDADT = parsedDADT.replaceAll(varToReplace, renameFunctionVariableForDES(derivativeVar));
				}
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
	
	public Map<String, String> getDerivativeVariableMap() {
		return derivativeVariableMap;
	}

	public Map<String, String> getDefinitionsParsingMap() {
		return definitionsParsingMap;
	}
}