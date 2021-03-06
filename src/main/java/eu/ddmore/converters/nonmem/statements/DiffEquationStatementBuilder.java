/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import crx.converter.spi.blocks.StructuralBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
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
        Map<String, String> derivativeVarDefs = dadtDefinitionsInDES;
        if(Formatter.isInDesBlock()){
            varDefinitions = appendAllVarDefinitionsWithSuffix(allVarDefinitions);
            derivativeVarDefs = appendAllVarDefinitionsWithSuffix(dadtDefinitionsInDES);
        }

        diffEqStatementBlock.append(getVariableDefinitionsStatement(varDefinitions));

        for(String var : derivativeVarDefs.keySet()){
            diffEqStatementBlock.append(Formatter.endline(derivativeVarDefs.get(var)));
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
            String variable = (Formatter.isInDesBlock())? Formatter.renameVarForDES(var):var;
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
            String variable = variableType.getSymbId().toUpperCase();
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
            String variable = definitionType.getSymbId().toUpperCase();
            String rhs = context.getLocalParserHelper().parse(definitionType);
            allVarDefinitions.put(variable, rhs);
        }
    }

    private void addDADTdefinitionsToDES(StructuralBlock block) {
        for(DerivativeVariable variableType: block.getStateVariables()){
            String parsedDADT = context.getLocalParserHelper().parse(variableType).toUpperCase();
            String variable = variableType.getSymbId().toUpperCase();

            if(isDerivativeVariableHasAmount(variable)){
                String index = context.getDerivativeVarCompSequences().get(variable);
                parsedDADT = parsedDADT.replaceFirst(variable+" =", "DADT("+index+") =");
                dadtDefinitionsInDES.put(variable, parsedDADT);
            }
        }
    }

    private Map<String, String> appendAllVarDefinitionsWithSuffix(Map<String, String> variableDefinitions){
        Map<String, String> varDefinitionsWithsuffix = new LinkedHashMap<String, String>();

        for(String variable : variableDefinitions.keySet()){
            String varDefinition = new String();
            if(varDefinitionsWithsuffix.containsKey(variable)){
                varDefinition = format(varDefinitionsWithsuffix.get(variable));
            }else {
                varDefinition = format(variableDefinitions.get(variable));
            }

            varDefinition = replaceTimeVarForDES(varDefinition);

            Map<String, String> reservedWords = context.getReservedWords();
            for(String revervedWord : reservedWords.keySet()){
                if(variable.equals(revervedWord) 
                        || variable.equals(reservedWords.get(revervedWord))){
                    varDefinition = replaceVariable(reservedWords.get(revervedWord), varDefinition);
                    varDefinitionsWithsuffix.put(variable, varDefinition);
                }
            }

            for(String derivativeVar : derivativeVarsInDES.keySet()){
                derivativeVar = format(derivativeVar);
                if(varDefinition.contains(derivativeVar)){
                    varDefinition = replaceVariable(derivativeVar, varDefinition);
                    varDefinitionsWithsuffix.put(variable, varDefinition);
                }
            }

            for(String derivativeVar : allVarDefinitions.keySet()){
                derivativeVar = (reservedWords.get(derivativeVar)!=null)?reservedWords.get(derivativeVar):derivativeVar;

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
        definition = definition.replaceAll(variablePatternToReplace, Formatter.renameVarForDES(variableToReplace));

        return definition;
    }

    private String replaceTimeVarForDES(String definition) {
        if(Formatter.isInDesBlock()){
            String varPatternToReplace = "\\b"+Pattern.quote(Formatter.ColumnConstant.TIME.toString())+"\\b";
            definition = definition.replaceAll(varPatternToReplace, Formatter.NmConstant.T.toString());
        }
        return definition;
    }

    private boolean isDerivativeVariableHasAmount(String variable) {
        return context.getDerivativeVarCompSequences().containsKey(variable);
    }

    public Map<String, String> getDerivativeVarsInDES() {
        return derivativeVarsInDES;
    }

    
    public Map<String, String> getDadtDefinitionsInDES() {
        return dadtDefinitionsInDES;
    }

    public Map<String, String> getAllVarDefinitions() {
        return allVarDefinitions;
    }
}