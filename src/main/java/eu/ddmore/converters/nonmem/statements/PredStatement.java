/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import crx.converter.engine.parts.BaseStep.MultipleDvRef;
import crx.converter.engine.parts.ConditionalDoseEvent;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.LocalVariableHandler;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 *
 */
public class PredStatement {

    private final ConversionContext context;
    private final PkMacroAnalyser analyser = new PkMacroAnalyser();

    public PredStatement(ConversionContext context){
        this.context = context;
    }

    public StringBuilder getPredStatement(){
        String statementName = Formatter.endline()+Formatter.pred();
        StringBuilder predStatement = new StringBuilder();
        PkMacroDetails pkMacroDetails = analyser.analyse(context);
        String advanType = pkMacroDetails.getMacroAdvanType();

        if(!context.getDerivativeVars().isEmpty()){
            if(advanType.isEmpty()){
                statementName = Formatter.endline()+Formatter.sub();
                predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
                predStatement.append(getDerivativePredStatement().toString());
            }else{
                //Advan PK macros
                predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+" TRANS=1"));
                predStatement.append(getPKStatement());
                predStatement.append(Formatter.error());
                predStatement.append(getErrorStatement());
            }
        }else{
            //non derivative pred block
            predStatement.append(Formatter.endline()+statementName);
            predStatement.append(getNonDerivativePredStatement());
        }
        return new StringBuilder(predStatement.toString().toUpperCase());
    }

    /**
     * Returns non derivative pred statement.
     * @return 
     */
    private StringBuilder getNonDerivativePredStatement() {
        StringBuilder nonDerivativePredBlock = new StringBuilder();
        LocalVariableHandler variableHandler = new LocalVariableHandler(context);

        nonDerivativePredBlock.append(getPredCoreStatement());
        nonDerivativePredBlock.append(getAllIndividualParamAssignments());
        nonDerivativePredBlock.append(variableHandler.getVarDefinitionTypesForNonDES()+Formatter.endline());
        nonDerivativePredBlock.append(context.getDiscreteHandler().getDiscreteStatement());
        nonDerivativePredBlock.append(getErrorStatement());

        return nonDerivativePredBlock;
    }

    /**
     * gets pred core statement for nonmem file.
     */
    private StringBuilder getPredCoreStatement() {
        StringBuilder predCoreBlock = new StringBuilder();
        predCoreBlock.append(getConditionalDoseDetails());
        predCoreBlock.append(context.buildThetaAssignments()+Formatter.endline());
        predCoreBlock.append(context.buildEtaAssignments()+Formatter.endline());
        predCoreBlock.append(getTransformedCovStatement());
        predCoreBlock.append(context.getSimpleParamAssignments()+Formatter.endline());

        return predCoreBlock;
    }

    private StringBuilder getDerivativePredStatement() {
        StringBuilder DerivativePredblock = new StringBuilder();
        DerivativePredblock.append(getModelStatement());
        //TODO : getAbbreviatedStatement();
        DerivativePredblock.append(getPKStatement());

        DerivativePredblock.append(Formatter.des());

        DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(context);        
        Formatter.setInDesBlock(true);
        DerivativePredblock.append(desBuilder.getDifferentialEquationsStatement());
        Formatter.setInDesBlock(false);

        //TODO: getAESStatement();
        DerivativePredblock.append(Formatter.endline()+Formatter.error());
        DerivativePredblock.append(getErrorStatement(desBuilder));

        return DerivativePredblock;
    }

    private String getConditionalDoseDetails() {
        List<ConditionalDoseEvent> conditionalDoseEvents = ScriptDefinitionAccessor.getAllConditionalDoseEvents(context.getScriptDefinition());

        StringBuilder doseEvent = new StringBuilder();
        for(ConditionalDoseEvent event : conditionalDoseEvents){
            String statement = context.getConditionalEventBuilder().parseConditionalDoseEvent(event);
            if(!StringUtils.isEmpty(statement)){
                doseEvent.append(statement);
            }
        }
        return doseEvent.toString();
    }

    /**
     * get Error statement for nonmem pred block
     * 
     * @return
     */
    private String getErrorStatement() {
        return getErrorStatement(null);
    }

    /**
     * Gets Error statement for nonmem pred block.
     * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
     * 
     * @param desBuilder
     * @return
     */
    private String getErrorStatement(DiffEquationStatementBuilder desBuilder) {

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
            SymbolRef columnName = context.getConditionalEventBuilder().getDVColumnReference(dvReference);

            if(columnName!=null && context.getErrorStatements().containsKey(columnName.getSymbIdRef())){

                String condition = context.getConditionalEventBuilder().getMultipleDvCondition(dvReference);
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
            String statement = context.getConditionalEventBuilder().buildConditionalStatement(condition, errorDetails.toString());
            errorBlock.append(statement);
        }else{
            errorBlock.append(errorDetails);
        }
        return errorBlock;
    }

    /**
     * gets pk block for pred statement
     */
    private StringBuilder getPKStatement() {
        StringBuilder pkStatementBlock = new StringBuilder();
        pkStatementBlock.append(Formatter.endline());
        pkStatementBlock.append(Formatter.pk());
        pkStatementBlock.append(getPredCoreStatement());
        pkStatementBlock.append(getAllIndividualParamAssignments());
        pkStatementBlock.append(getDifferentialInitialConditions());
        return new StringBuilder(pkStatementBlock.toString().toUpperCase());
    }

    /**
     * get model statement block for pred statement of nonmem file.
     * 
     */
    private StringBuilder getModelStatement() {
        StringBuilder modelBlock = new StringBuilder();
        modelBlock.append(Formatter.endline());
        modelBlock.append(Formatter.model());
        for(DerivativeVariable stateVariable :context.getDerivativeVars()){
            String compartment = stateVariable.getSymbId().toUpperCase();
            modelBlock.append(Formatter.endline("COMP "+"(COMP"+context.getDerivativeVarCompSequences().get(compartment)+") "+Formatter.addComment(compartment)));
        }
        return modelBlock;
    }

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return
     */
    private StringBuilder getDifferentialInitialConditions(){
        StringBuilder builder = new StringBuilder();
        if(!context.getScriptDefinition().getStructuralBlocks().isEmpty())
            builder = InitConditionBuilder.getDifferentialInitialConditions(context.getScriptDefinition().getStructuralBlocks());	
        return builder;
    }

    /**
     * This method will collect all the parsing for Individual parameter assignments.
     *  
     * @param blocks
     * @return
     */
    private StringBuilder getAllIndividualParamAssignments() {
        StringBuilder individualParamAssignmentBlock = new StringBuilder();
        IndividualDefinitionEmitter individualDefEmitter = new IndividualDefinitionEmitter(context);
        for(ParameterBlock parameterBlock : context.getScriptDefinition().getParameterBlocks()){
            for(IndividualParameter parameterType: parameterBlock.getIndividualParameters()){
                individualParamAssignmentBlock.append(individualDefEmitter.createIndividualDefinition(parameterType));
            }
        }
        return individualParamAssignmentBlock;
    }

    private StringBuilder getTransformedCovStatement() {
        StringBuilder transformedCovBlock = new StringBuilder();
        //Find and add transformed covariates before indiv parameter definitions 
        List<CovariateDefinition> covDefs = context.getLexer().getCovariates();
        for(CovariateDefinition covDef : covDefs){
            if(covDef.getContinuous()!=null){
                ContinuousCovariate contCov = covDef.getContinuous();
                for(CovariateTransformation transformation : contCov.getListOfTransformation()){
                    String transCovDefinition = context.getParser().getSymbol(transformation);
                    transformedCovBlock.append(Formatter.endline(transCovDefinition));
                }
            }
        }
        return transformedCovBlock;
    }

    public PkMacroAnalyser getAnalyser() {
        return analyser;
    }
}
