/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.Parser;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ErrorModel;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class PredStatement {
	
    private ScriptDefinition scriptDefinition;
    private List<DerivativeVariable> derivativeVarList = new ArrayList<DerivativeVariable>();
    private List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();
	private Parser parser;
	public static Boolean isDES = false;

	public PredStatement(ScriptDefinition scriptDefinition, Parser parser){
		this.parser = parser;
		this.scriptDefinition = scriptDefinition;
		derivativeVarList.addAll(getAllStateVariables());
	}
	
	public void getPredStatement(PrintWriter fout){
		String statementName = Formatter.endline()+Formatter.pred();
		if(!derivativeVarList.isEmpty()){
			//TODO: Add $SUB block. need to have details around it.
			statementName = Formatter.endline()+Formatter.sub();
			fout.write(Formatter.endline()+Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
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
        errorStatements = prepareAllErrorStatements();
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
		DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(scriptDefinition, errorStatements, parser);
		DerivativePredblock.append(desBuilder.getDifferentialEquationsStatement(derivativeVarList));
		isDES = false;
		//TODO: getAESStatement();
		DerivativePredblock.append(getErrorStatement(desBuilder.getDefinitionsParsingMap(), desBuilder.getDerivativeVariableMap()));
        
        return DerivativePredblock;
	}
	
    /**
     * get Error statement for nonmem pred block
     * 
     * @return
     */
    private String getErrorStatement() {
        return getErrorStatement(null, null);
    }
	
	/**
	 * get Error statement for nonmem pred block
	 * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
	 * @return 
	 * 
	 */
	private String getErrorStatement(Map<String, String> definitionsParsingMap, Map<String, String> derivativeVariableMap) {
		StringBuilder errorBlock = new StringBuilder();
		errorBlock.append(Formatter.endline());
		errorBlock.append(Formatter.error());
		for(ErrorStatement errorStatement: errorStatements){
			if(definitionsParsingMap != null){
				errorBlock.append(errorStatement.getDetailsForDES(definitionsParsingMap,derivativeVariableMap));
			}else{
				errorBlock.append(errorStatement.getErrorStatementDetails());
			}
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
			ObservationError errorType = block.getObservationError();
			if(errorType instanceof GeneralObsError){
//				GeneralObsError genError = (GeneralObsError) errorType;
//				TODO : DDMORE-1013 : add support for general observation error type once details are available
//			    throw new IllegalArgumentException("general observation error type is not yet supported.");
			}
			if(errorType instanceof GaussianObsError){
				GaussianObsError error = (GaussianObsError) errorType;
				ErrorModel errorModel = error.getErrorModel();
				String output = error.getOutput().getSymbRef().getSymbIdRef();
				FunctionCallType functionCall = errorModel.getAssign().getEquation().getFunctionCall();
				
				ErrorStatement errorStatement = new ErrorStatement(functionCall, output);
				errorStatements.add(errorStatement);
			}else{
//				TODO : Check if there are any other types to encounter
			}
		}
		return errorStatements;
	}

	/**
	 * gets pk block for pred statement
	 */
	private StringBuilder getPKStatement() {
		StringBuilder pkStatementBlock = new StringBuilder();
		pkStatementBlock.append(Formatter.endline());
		pkStatementBlock.append(Formatter.pk());
		pkStatementBlock.append(getPredCoreStatement());
		pkStatementBlock.append(getDifferentialInitialConditions());
		return pkStatementBlock;
	}

	/**
	 * get model statement block for pred statement of nonmem file.
	 * 
	 */
	private StringBuilder getModelStatement() {
		StringBuilder modelBlock = new StringBuilder();
		modelBlock.append(Formatter.endline());
		modelBlock.append(Formatter.model());
		for(DerivativeVariable stateVariable :getAllStateVariables()){
			modelBlock.append(Formatter.endline("COMP "+"("+Formatter.addPrefix(stateVariable.getSymbId())+")"));
		}
		return modelBlock;
	}
	
	/**
	 * Collects all derivativeVariable types (state variables) from structural blocks in order to create model statement.
	 * 
	 * @return
	 */
	private Set<DerivativeVariable> getAllStateVariables() {
		Set<DerivativeVariable> stateVariables = new LinkedHashSet<DerivativeVariable>();
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
		if(!scriptDefinition.getStructuralBlocks().isEmpty())
			builder = InitConditionBuilder.getDifferentialInitialConditions(scriptDefinition.getStructuralBlocks());	
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
			for(IndividualParameter parameterType: parameterBlock.getIndividualParameters()){
				IndividualParamAssignmentBlock.append(parser.createIndividualDefinition(parameterType));	
			}
		}
		return IndividualParamAssignmentBlock;
	}
}
