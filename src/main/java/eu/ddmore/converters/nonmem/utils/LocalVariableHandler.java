/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.Map;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;

/**
 * This class retrieves local variable definitions from structural blocks.
 *  
 */
public class LocalVariableHandler {
    ConversionContext context;
    public LocalVariableHandler(ConversionContext context){
        this.context = context;
    }

    /**
     * 
     * @return
     */
    public StringBuilder getVarDefinitionTypesForNonDES(){
        StringBuilder varDefinitionsBlock = new StringBuilder();
        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){
            if(!block.getLocalVariables().isEmpty()){
                varDefinitionsBlock.append(addVarDefinitionTypes(block, null));
            }
        }
        return varDefinitionsBlock;
    }

    /**
     * This method gets variable definitions for the variables and adds them to statement.
     * As workaround to issues with variables used in Error model, we rename those variables in DES block.
     * 
     * @param block
     * @param varDefinitionsInDES
     * @return
     */
    public StringBuilder addVarDefinitionTypes(StructuralBlock block, Map<String, String> varDefinitionsInDES) {

        StringBuilder varDefinitionsBlock = new StringBuilder();

        for (VariableDefinition definitionType: block.getLocalVariables()){
            String variable = Formatter.addPrefix(definitionType.getSymbId());
            String lhs = "";
            String rhs = context.parse(definitionType);

            if(Formatter.isInDesBlock()){
                if(rhs.startsWith(variable+" =")) {
                    if(isVarFromErrorFunction(variable)){
                        rhs = rhs.replaceFirst(variable+" =","");
                        Preconditions.checkNotNull(varDefinitionsInDES,"variable definitions map needs to be provided to add local variable definition for DES.");
                        varDefinitionsInDES.put(variable, rhs);
                        lhs = DiffEquationStatementBuilder.renameFunctionVariableForDES(variable);
                    }
                }
            }

            if(lhs.isEmpty()){
                varDefinitionsBlock.append(rhs);
            }else {
                varDefinitionsBlock.append(lhs+" = "+rhs);
            }
        }
        return varDefinitionsBlock;
    }

    /**
     * Determines if variable is function variable from error model.
     * 
     * @param variable
     * @return
     */
    public Boolean isVarFromErrorFunction(String variable){
        for(ErrorStatement errorStatement : context.getErrorStatements()){
            if(errorStatement.getFunction().equals(variable)){
                return true;
            }
        }
        return false;
    }
}
