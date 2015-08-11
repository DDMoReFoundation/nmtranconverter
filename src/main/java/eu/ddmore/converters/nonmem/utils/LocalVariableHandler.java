/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;

/**
 * This class retrieves local variable definitions from structural blocks.
 *  
 */
public class LocalVariableHandler {
    private final ConversionContext context;

    public LocalVariableHandler(ConversionContext context){
        this.context = context;
    }

    /**
     * This method gets variable definitions for the non DES variables and adds them to statement.
     * 
     * @return string builder variable definitions types
     */
    public StringBuilder getVarDefinitionTypesForNonDES(){
        StringBuilder varDefinitionsBlock = new StringBuilder();
        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){

            if(!block.getLocalVariables().isEmpty()){
                for (VariableDefinition definitionType: block.getLocalVariables()){
                    String rhs = context.parse(definitionType);
                    if(!Formatter.isInDesBlock())
                        varDefinitionsBlock.append(rhs);
                }
            }
        }
        return varDefinitionsBlock;
    }

    /**
     * Determines if variable is function variable from error model.
     * 
     * @param variable
     * @return boolean result
     */
    public Boolean isVarFromErrorFunction(String variable){
        for(ErrorStatement errorStatement : context.getErrorStatements().values()){
            if(errorStatement.getFunction().equals(variable)){
                return true;
            }
        }
        return false;
    }
}
