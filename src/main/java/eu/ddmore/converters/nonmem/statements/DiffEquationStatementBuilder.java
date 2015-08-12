/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;

/**
 * This class builds DES statement using derivative variables and variable definitions.
 * 
 */
public class DiffEquationStatementBuilder {
    //it will hold definition types and its parsed equations which we will need to add in Error statement as well.
    private final Map<String, String> derivativeVarsInDES = new LinkedHashMap<String, String>();
    private final Map<String, String> allVarDefinitions = new LinkedHashMap<String, String>();
    private final Map<String, String> dadtDefinitionsInDES = new LinkedHashMap<String, String>();
    private final ConversionContext context;
    private static final String DES_VAR_SUFFIX = "_"+Block.DES.toString();

    public DiffEquationStatementBuilder(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null.");
        this.context = context;
        initialiseDiffEquationStatement();
    }

    /**
     * Gets DES statement for derivative pred block.
     * 
     * @return differential equation statement
     */
    public StringBuilder getDifferentialEquationsStatement() {
        StringBuilder diffEqStatementBlock = new StringBuilder();

        Map<String, String> varDefinitions = allVarDefinitions;
        Map<String, String> derivatibeVarDefs = dadtDefinitionsInDES;
        if(Formatter.isInDesBlock()){
            varDefinitions = appendAllVarDefinitionsWithsuffix(allVarDefinitions);
            derivatibeVarDefs = appendAllVarDefinitionsWithsuffix(dadtDefinitionsInDES);
        }

        diffEqStatementBlock.append(getVariableDefinitionsStatement(varDefinitions));

        for(String var : derivatibeVarDefs.keySet()){
            diffEqStatementBlock.append(Formatter.endline(derivatibeVarDefs.get(var)));
        }
        return diffEqStatementBlock;
    }

    /**
     * Gets variable definitions to add to DES as well as to Error statement.
     *  
     * @param varDefinitions
     * @return
     */
    public StringBuilder getVariableDefinitionsStatement(Map<String, String> varDefinitions) {
        StringBuilder diffEqStatementBlock = new StringBuilder();

        for(String var : derivativeVarsInDES.keySet()){
            String variable = (Formatter.isInDesBlock())?renameVarForDES(var):var;
            String derivativeVarStatement = variable+" = "+derivativeVarsInDES.get(var);
            diffEqStatementBlock.append(Formatter.endline(derivativeVarStatement));
        }
        Formatter.endline();

        for(String var : varDefinitions.keySet()){
            String varDefinition = format(varDefinitions.get(var));
            diffEqStatementBlock.append(Formatter.endline(varDefinition));
        }
        Formatter.endline();
        return diffEqStatementBlock;
    }

    private void initialiseDiffEquationStatement() {

        for (DerivativeVariable variableType : context.getDerivativeVars()){
            String variable = Formatter.addPrefix(variableType.getSymbId());
            String varAmount = Formatter.getVarAmountFromCompartment(variable, context.getDerivativeVarCompSequences());
            if(!varAmount.isEmpty()){
                derivativeVarsInDES.put(variable, varAmount);
            }
        }

        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){
            addAllVarDefinitionTypes(block);
            addDADTdefinitionsToDES(block);
        }
    }

    private void addAllVarDefinitionTypes(StructuralBlock block) {
        for (VariableDefinition definitionType: block.getLocalVariables()){
            String variable = Formatter.addPrefix(definitionType.getSymbId());
            String rhs = context.parse(definitionType);
            allVarDefinitions.put(variable, rhs);
        }
    }

    private void addDADTdefinitionsToDES(StructuralBlock block) {
        for(DerivativeVariable variableType: block.getStateVariables()){
            String parsedDADT = context.parse(variableType).toUpperCase();
            String variable = Formatter.addPrefix(variableType.getSymbId());

            if(isDerivativeVariableHasAmount(variable)){
                String index = context.getDerivativeVarCompSequences().get(variable);
                parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
                dadtDefinitionsInDES.put(variable, parsedDADT);
            }
        }
    }

    private Map<String, String> appendAllVarDefinitionsWithsuffix(Map<String, String> variableDefinitions){
        Map<String, String> varDefinitionsWithsuffix = new LinkedHashMap<String, String>();

        for(String variable : variableDefinitions.keySet()){
            String varDefinition = new String();
            if(varDefinitionsWithsuffix.containsKey(variable)){
                varDefinition = format(varDefinitionsWithsuffix.get(variable));
            }else {
                varDefinition = format(variableDefinitions.get(variable));
            }

            for(String derivativeVar : derivativeVarsInDES.keySet()){
                derivativeVar = format(derivativeVar);
                if(varDefinition.contains(derivativeVar)){
                    varDefinition = replaceVariable(derivativeVar, varDefinition);
                    varDefinitionsWithsuffix.put(variable, varDefinition);
                }
            }

            for(String derivativeVar : allVarDefinitions.keySet()){
                if(format(varDefinition).contains(format(derivativeVar))){
                    varDefinition = replaceVariable(derivativeVar, varDefinition);
                    varDefinitionsWithsuffix.put(variable, varDefinition);
                }
            }
        }
        return varDefinitionsWithsuffix;
    }

    private String format(String variable){
        return variable.trim().toUpperCase();
    }

    private String replaceVariable(String variableToReplace, String definition){
        String variablePatternToReplace = "\\b"+Pattern.quote(variableToReplace)+"\\b";
        return definition.replaceAll(variablePatternToReplace, renameVarForDES(variableToReplace));
    }

    private boolean isDerivativeVariableHasAmount(String variable) {
        return context.getDerivativeVarCompSequences().containsKey(variable);
    }

    private String renameVarForDES(String variable) {
        variable = variable+DES_VAR_SUFFIX;
        return variable; 
    }

    public Map<String, String> getDerivativeVarsInDES() {
        return derivativeVarsInDES;
    }


    public Map<String, String> getAllVarDefinitions() {
        return allVarDefinitions;
    }
}