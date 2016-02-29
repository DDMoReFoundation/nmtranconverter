/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.BaseStep.MultipleDvRef;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.ErrorStatementEmitter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError.ErrorModel;

/**
 * Handles error statement generation for nmtran.
 */
public class ErrorStatementHandler {

    private final Map<String,ErrorStatement> errorStatements = new HashMap<String,ErrorStatement>();
    ConversionContext context;

    public ErrorStatementHandler(ConversionContext context) {
        this.context = context;
        prepareAllErrorStatements();
    }

    /**
     * This method will list prepare all the error statements and returns the list.
     * We need to prepare this list separately as we need to use it in DES block before writing out to ERROR block.
     * @return
     */
    private Map<String,ErrorStatement> prepareAllErrorStatements(){

        for(ObservationBlock block : context.getScriptDefinition().getObservationBlocks()){
            ObservationError errorType = block.getObservationError();
            if(errorType instanceof GeneralObsError){
                //              GeneralObsError genError = (GeneralObsError) errorType;
                //              TODO : DDMORE-1013 : add support for general observation error type once details are available
                //              throw new IllegalArgumentException("general observation error type is not yet supported.");
            }
            if(errorType instanceof StructuredObsError){
                StructuredObsError error = (StructuredObsError) errorType;
                ErrorStatement errorStatement = prepareErrorStatement(error);
                errorStatements.put(error.getSymbId(), errorStatement);
            }else{
                //              TODO : Check if there are any other types to encounter
            }
        }
        return errorStatements;
    }

    /**
     * Prepares and returns error statement for the structured observation error.
     * 
     * @param error
     * @return
     */
    private ErrorStatement prepareErrorStatement(StructuredObsError error) {
        ErrorModel errorModel = error.getErrorModel();
        String output = error.getOutput().getSymbRef().getSymbIdRef();
        FunctionCallType functionCall = errorModel.getAssign().getFunctionCall();

        ErrorStatement errorStatement = new ErrorStatement(functionCall, output);
        return errorStatement;
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
            for(ErrorStatement error : errorStatements.values()){
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

            if(columnName!=null && errorStatements.containsKey(columnName.getSymbIdRef())){

                String condition = context.getConditionalEventHandler().getMultipleDvCondition(dvReference);
                ErrorStatement errorStatement = errorStatements.get(columnName.getSymbIdRef());
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