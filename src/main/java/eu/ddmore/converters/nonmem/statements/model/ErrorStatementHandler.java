/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import crx.converter.engine.parts.BaseStep.MultipleDvRef;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.ErrorStatementEmitter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

/**
 * Handles error statement generation for nmtran.
 */
public class ErrorStatementHandler {

    ConversionContext context;

    public ErrorStatementHandler(ConversionContext context) {
        this.context = context;
    }

    /**
     * get Error statement for nonmem pred block
     * 
     * @return
     */
    public String getErrorStatement() {
        return getErrorStatement(null);
    }

    /**
     * Gets Error statement for nonmem pred block.
     * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
     * 
     * @param desBuilder
     * @return
     */
    public String getErrorStatement(DiffEquationStatementBuilder desBuilder) {

        StringBuilder errorBlock = new StringBuilder();
        if(desBuilder!=null){
            errorBlock.append(desBuilder.getVariableDefinitionsStatement(desBuilder.getAllVarDefinitions()));
        }

        StringBuilder errorBlockWithMDV = getErrorBlockForMultipleDV();

        if(!errorBlockWithMDV.toString().isEmpty()){
            errorBlock.append(errorBlockWithMDV);
        }else{
            for(ErrorStatement error : context.getErrorStatements().values()){
                ErrorStatementEmitter statementEmitter = new ErrorStatementEmitter(error);
                errorBlock.append(statementEmitter.getErrorStatementDetails());
            }
        }
        return errorBlock.toString();
    }

    private StringBuilder getErrorBlockForMultipleDV() {
        StringBuilder errorBlockWithMDV = new StringBuilder();
        List<MultipleDvRef> multipleDvReferences = ScriptDefinitionAccessor.getAllMultipleDvReferences(context.getScriptDefinition());
        for(MultipleDvRef dvReference : multipleDvReferences){
            SymbolRef columnName = context.getConditionalEventHandler().getDVColumnReference(dvReference);

            if(columnName!=null && context.getErrorStatements().containsKey(columnName.getSymbIdRef())){

                String condition = context.getConditionalEventHandler().getMultipleDvCondition(dvReference);
                ErrorStatement errorStatement = context.getErrorStatements().get(columnName.getSymbIdRef());
                errorBlockWithMDV.append(getErrorStatementForMultipleDv(errorStatement, condition));
            }
        }

        return errorBlockWithMDV;
    }

    private StringBuilder getErrorStatementForMultipleDv(
            ErrorStatement errorStatement, String condition) {
        StringBuilder errorBlock = new StringBuilder();
        ErrorStatementEmitter statementEmitter = new ErrorStatementEmitter(errorStatement);
        StringBuilder errorDetails = statementEmitter.getErrorStatementDetails();

        if(!StringUtils.isEmpty(condition)){
            String statement = context.getConditionalEventHandler().buildConditionalStatement(condition, errorDetails.toString());
            errorBlock.append(statement);
        }else{
            errorBlock.append(errorDetails);
        }
        return errorBlock;
    }
}