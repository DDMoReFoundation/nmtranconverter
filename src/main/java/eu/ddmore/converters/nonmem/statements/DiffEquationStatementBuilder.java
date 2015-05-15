/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;

public class DiffEquationStatementBuilder {
    private static final String DES = "DES";
    private final String DES_VAR_SUFFIX = "_"+DES;
    //it will hold definition types and its parsed equations which we will need to add in Error statement as well.
    private Map<String, String> definitionsParsingMap = new HashMap<String, String>();
    ConversionContext context;

    public DiffEquationStatementBuilder(ConversionContext context) {
        this.context = context;
    }

    /**
     * gets DES block for pred statement
     * 
     */
    public StringBuilder getDifferentialEquationsStatement() {
        StringBuilder diffEqStatementBlock = new StringBuilder();
        diffEqStatementBlock.append(Formatter.des());
        for (DerivativeVariable variableType : context.getDerivativeVars()){
            String variable = Formatter.addPrefix(variableType.getSymbId());

            String varAmount = ConversionContext.getVarAmountFromCompartment(variable, context.getDerivativeVarCompSequences());
            if(!varAmount.isEmpty())
                diffEqStatementBlock.append(Formatter.endline(variable+" = "+varAmount));
            if(isVarFromErrorFunction(variable))
                definitionsParsingMap.put(variable, varAmount);
        }
        diffEqStatementBlock.append(Formatter.endline());
        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){
            diffEqStatementBlock.append(addVarDefinitionTypesToDES(block));
            diffEqStatementBlock.append(addDerivativeVarToDES(block));
        }
        return diffEqStatementBlock;
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
        for (VariableDefinition definitionType: block.getLocalVariables()){
            String variable = Formatter.addPrefix(definitionType.getSymbId());
            String rhs = context.parse(definitionType);
            if(rhs.startsWith(variable+" =")) {
                rhs = rhs.replaceFirst(variable+" =","");
                if(isVarFromErrorFunction(variable)){
                    definitionsParsingMap.put(variable, rhs);
                    variable = renameFunctionVariableForDES(variable);
                }
                varDefinitionsBlock.append(variable+" = "+rhs);
            }else{
                varDefinitionsBlock.append(Formatter.endline(rhs));
            }
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
        for(DerivativeVariable variableType: block.getStateVariables()){
            String parsedDADT = context.parse(variableType);
            String variable = Formatter.addPrefix(variableType.getSymbId());
            if(isDerivativeVariableHasAmount(variable)){
                String index = context.getDerivativeVarCompSequences().get(variable);
                parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
            }
            for(String derivativeVar : definitionsParsingMap.keySet()){
                String varToReplace = new String("\\b"+Pattern.quote(derivativeVar)+"\\b");
                if(!isDerivativeVariableHasAmount(derivativeVar)){
                    parsedDADT = parsedDADT.replaceAll(varToReplace, renameFunctionVariableForDES(derivativeVar));
                }
            }
            derivativeVarBlock.append(parsedDADT);
        }
        return derivativeVarBlock;
    }

    private boolean isDerivativeVariableHasAmount(String variable) {
        return context.getDerivativeVarCompSequences().containsKey(variable);
    }

    /**
     * Determines if variable is function variable from error model.
     * 
     * @param variable
     * @return
     */
    private Boolean isVarFromErrorFunction(String variable){
        for(ErrorStatement errorStatement : context.getErrorStatements()){
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

    public Map<String, String> getDefinitionsParsingMap() {
        return definitionsParsingMap;
    }
}