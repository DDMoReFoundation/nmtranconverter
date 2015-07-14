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
import eu.ddmore.converters.nonmem.utils.LocalVariableHandler;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;

public class DiffEquationStatementBuilder {
    //it will hold definition types and its parsed equations which we will need to add in Error statement as well.
    private final Map<String, String> varDefinitionsInDES = new HashMap<String, String>();
    private final ConversionContext context;

    public DiffEquationStatementBuilder(ConversionContext context) {
        this.context = context;
    }

    /**
     * gets DES block for pred statement
     * 
     */
    public StringBuilder getDifferentialEquationsStatement() {
        StringBuilder diffEqStatementBlock = new StringBuilder();
        LocalVariableHandler variableHandler = new LocalVariableHandler(context);

        for (DerivativeVariable variableType : context.getDerivativeVars()){
            String variable = Formatter.addPrefix(variableType.getSymbId());

            String varAmount = Formatter.getVarAmountFromCompartment(variable, context.getDerivativeVarCompSequences());
            if(!varAmount.isEmpty())
                diffEqStatementBlock.append(Formatter.endline(variable+" = "+varAmount));
            if(variableHandler.isVarFromErrorFunction(variable))
                varDefinitionsInDES.put(variable, varAmount);
        }
        diffEqStatementBlock.append(Formatter.endline());
        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){
            diffEqStatementBlock.append(variableHandler.addVarDefinitionTypes(block,varDefinitionsInDES));
            diffEqStatementBlock.append(addDerivativeVarToDES(block));
        }
        return diffEqStatementBlock;
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
            String parsedDADT = context.parse(variableType).toUpperCase();
            String variable = Formatter.addPrefix(variableType.getSymbId());
            if(isDerivativeVariableHasAmount(variable)){
                String index = context.getDerivativeVarCompSequences().get(variable);
                parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
            }
            for(String derivativeVar : varDefinitionsInDES.keySet()){
                String varToReplace = "\\b"+Pattern.quote(derivativeVar)+"\\b";
                if(!isDerivativeVariableHasAmount(derivativeVar)){
                    parsedDADT = parsedDADT.replaceAll(varToReplace, Formatter.renameFunctionVariableForDES(derivativeVar));
                }
            }
            derivativeVarBlock.append(parsedDADT);
        }
        return derivativeVarBlock;
    }

    private boolean isDerivativeVariableHasAmount(String variable) {
        return context.getDerivativeVarCompSequences().containsKey(variable);
    }

    public Map<String, String> getVarDefinitions() {
        return varDefinitionsInDES;
    }
}