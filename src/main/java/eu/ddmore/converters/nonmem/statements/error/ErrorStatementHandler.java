/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.common.MultipleDvRef;
import crx.converter.spi.blocks.ObservationBlock;
import crx.converter.tree.TreeMaker;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLElement;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousObservationModel;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError.ErrorModel;

/**
 * Handles error statement generation for nmtran.
 */
public class ErrorStatementHandler {

    private final Map<String, ErrorStatement> errorStatements = new HashMap<String, ErrorStatement>();
    ConversionContext context;

    public ErrorStatementHandler(ConversionContext context) {
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context = context;
        prepareAllErrorStatements();
    }

    /**
     * This method will list prepare all the error statements and returns the list.
     * We need to prepare this list separately as we need to use it in DES block before writing out to ERROR block.
     * @return
     */
    private void prepareAllErrorStatements(){
        for(ObservationBlock block : context.getScriptDefinition().getObservationBlocks()){
            ObservationError errorType = block.getObservationError();
            if(errorType instanceof GeneralObsError){
                GeneralObsError error = (GeneralObsError) errorType;
                ContinuousObservationModel contModel = block.getModel().getContinuousData();
                ErrorStatement errorStatement = prepareErrorStatement(contModel, error);
                errorStatements.put(error.getSymbId(), errorStatement);
            }
            if(errorType instanceof StructuredObsError){
                StructuredObsError error = (StructuredObsError) errorType;
                ErrorStatement errorStatement = prepareErrorStatement(error);
                errorStatements.put(error.getSymbId(), errorStatement);
            }
        }
    }

    private ErrorStatement prepareErrorStatement(ContinuousObservationModel contModel, GeneralObsError error) {
        Map<String, String> varEquations = new HashMap<>();
        LocalParserHelper parserHelper = new LocalParserHelper(context);
        TreeMaker treeMaker = context.getLexer().getTreeMaker();

        for(PharmMLElement element : contModel.getListOfObservationModelElement()){
            String[] equation = parserHelper.parse(element, treeMaker.newInstance(element)).split("=");
            varEquations.put(equation[0], equation[1]);
        }
        String rhs = parserHelper.getParsedValueForRhs(error.getAssign());
        varEquations.put(error.getSymbId(), rhs);

        GeneralObsErrorStatement errorStatement = new GeneralObsErrorStatement(varEquations, false);
        return errorStatement;
    }

    /**
     * Prepares and returns error statement for the structured observation error.
     * @param error structured observational error
     * @return error statement
     */
    private ErrorStatement prepareErrorStatement(StructuredObsError error) {
        ErrorModel errorModel = error.getErrorModel();
        String output = error.getOutput().getSymbRef().getSymbIdRef();
        FunctionCallType functionCall = errorModel.getAssign().getFunctionCall();

        String epsilonVar = error.getResidualError().getSymbRef().getSymbIdRef();
        StructuralObsErrorStatement errorStatement = new StructuralObsErrorStatement(functionCall, output, epsilonVar, true);
        return errorStatement;
    }

    /**
     * get Error statement for nonmem pred block
     * @return error statement
     */
    public String getErrorStatement() {
        return getErrorStatement(null);
    }

    /**
     * Gets Error statement for nonmem pred block.
     * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
     * 
     * @param desBuilder
     * @return error statement
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
                errorBlock.append(error.getErrorStatement());
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

    private StringBuilder getErrorStatementForMultipleDv(ErrorStatement errorStatement, String condition) {
        StringBuilder errorBlock = new StringBuilder();

        if(!StringUtils.isEmpty(condition)){
            String statement = context.getConditionalEventHandler().buildConditionalStatement(condition, errorStatement.getErrorStatement().toString());
            errorBlock.append(statement);
        }else{
            errorBlock.append(errorStatement.getErrorStatement());
        }
        return errorBlock;
    }

    public Map<String, ErrorStatement> getErrorStatements() {
        return errorStatements;
    }
}