/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import crx.converter.engine.parts.BaseStep.MultipleDvRef;
import crx.converter.engine.parts.ConditionalDoseEvent;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.TemporalDoseEvent;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroAttribute;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.LocalVariableHandler;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;

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
        StringBuilder predStatement = new StringBuilder();
        PkMacroDetails pkMacroDetails = analyser.analyse(context);

        if(context.getDiscreteHandler().isDiscrete()){
            //Discrete
            if(context.getDiscreteHandler().isCountData()){
                predStatement.append(getModelStatementForCountData());
            }
            else if(context.getDiscreteHandler().isTimeToEventData()){
                predStatement.append(getModelStatementForTTE());
            }
            else if(context.getDiscreteHandler().isCategoricalData()){
                //TODO: add support for categorical data
            }
        }else if(!context.getDerivativeVars().isEmpty()){
            //DES
            //TODO: Handle specific types of advans. Currently everything goes through default advan type.
            //if(pkMacroDetails.getMacroAdvanType().isEmpty()){
            int tolValue = (context.getEstimationEmitter().isSAEM())? 6:9;
            predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+"ADVAN13 TOL="+tolValue));
            predStatement.append(getDerivativePredStatement(pkMacroDetails));
            //}else{
            //     //Advan PK macros
            //     predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+" TRANS=1"));
            //     predStatement.append(getPKStatement());
            //     predStatement.append(Formatter.error());
            //     predStatement.append(getErrorStatement());
            // }
        }else{
            //PRED
            predStatement.append(Formatter.endline()+Formatter.endline()+Formatter.pred());
            predStatement.append(getNonDerivativePredStatement());
        }
        return new StringBuilder(predStatement.toString().toUpperCase());
    }

    private DiffEquationStatementBuilder getDiffEquationStatement(StringBuilder statement) {
        statement.append(Formatter.des());

        DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(context);
        Formatter.setInDesBlock(true);
        statement.append(desBuilder.getDifferentialEquationsStatement());
        Formatter.setInDesBlock(false);

        return desBuilder;
    }

    private StringBuilder getPkMacroEquations(PkMacroDetails pkMacroDetails) {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();

        if(!pkMacroDetails.getAbsorptionOrals().isEmpty()){
            for(AbsorptionOralMacro oralMacro : pkMacroDetails.getAbsorptionOrals()){
                for(MacroValue value : oralMacro.getListOfValue()){
                    String macroEquation = getAbsOralMacroEquation(pkMacroDetails, value);
                    if(StringUtils.isNotEmpty(macroEquation)){
                        builder.append(Formatter.endline(macroEquation));
                    }
                }
            }
            builder.append(Formatter.endline());
        }

        return builder;
    }

    private String getAbsOralMacroEquation(PkMacroDetails pkMacroDetails, MacroValue value) {
        String valueArgument = value.getArgument().toUpperCase().trim();
        if(StringUtils.isNotEmpty(valueArgument) 
                && !valueArgument.equals(PkMacroAttribute.KA.name()) 
                && value.getAssign().getSymbRef()!=null) {

            PkMacroAttribute attribute= PkMacroAttribute.valueOf(valueArgument);
            //TODO : check for reserved word and add NM_ prefix 
            String variable = value.getAssign().getSymbRef().getSymbIdRef();
            return attribute.getValue()+ pkMacroDetails.getAbsOralCompNumber()+ " = "+ variable;
        }
        return "";
    }

    private StringBuilder getModelStatementForCountData() {
        StringBuilder countDataBlock = new StringBuilder();
        LocalVariableHandler variableHandler = new LocalVariableHandler(context);
        //PRED
        countDataBlock.append(Formatter.endline()+Formatter.endline()+Formatter.pred());
        countDataBlock.append(getPredCoreStatement());
        countDataBlock.append(getAllIndividualParamAssignments());
        countDataBlock.append(variableHandler.getVarDefinitionTypesForNonDES()+Formatter.endline());
        countDataBlock.append(context.getDiscreteHandler().getDiscreteStatement());
        countDataBlock.append(getErrorStatement());

        return countDataBlock;
    }

    private StringBuilder getModelStatementForTTE() {
        StringBuilder tteBlock = new StringBuilder();
        tteBlock.append(Formatter.endline());
        //$SUBS
        tteBlock.append(Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
        //$MODEL
        tteBlock.append(Formatter.endline());
        String modelStatement = Formatter.endline("$MODEL"+ "\n\t; One hardcoded compartment ") +
                Formatter.endline(Formatter.indent("COMP=COMP1"));
        tteBlock.append(modelStatement);
        //$PK                
        tteBlock.append(Formatter.endline()+Formatter.pk()); 
        tteBlock.append(getPredCoreStatement());
        tteBlock.append(getAllIndividualParamAssignments());
        //$DES
        DiffEquationStatementBuilder desBuilder = getDiffEquationStatement(tteBlock);
        String HAZARD_FUNC_DES = Formatter.renameVarForDES(context.getDiscreteHandler().getHazardFunction());
        tteBlock.append(Formatter.endline("HAZARD_FUNC_DES = "+HAZARD_FUNC_DES));
        tteBlock.append(Formatter.endline("DADT(1) = HAZARD_FUNC_DES"));
        //Customised ERROR
        //$ERROR
        tteBlock.append(Formatter.endline()+Formatter.error());
        if(desBuilder!=null){
            tteBlock.append(desBuilder.getVariableDefinitionsStatement(desBuilder.getAllVarDefinitions()));
        }
        tteBlock.append(Formatter.endline("CUMHAZ=A(1)        ; CUMHAZ since last event"));
        tteBlock.append(Formatter.endline("HAZARD_FUNC = "+context.getDiscreteHandler().getHazardFunction()));
        tteBlock.append(context.getDiscreteHandler().getDiscreteStatement());

        tteBlock.append(getErrorStatement());
        return tteBlock;
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

        if(!(context.isSigmaPresent() 
                || context.isOmegaForIIVPresent() || context.isOmegaForIOVPresent())){
            predCoreBlock.append(Formatter.getDummyEtaStatement());
        }else{
            predCoreBlock.append(context.buildEtaAssignments()+Formatter.endline());
        }
        predCoreBlock.append(getTransformedCovStatement());
        predCoreBlock.append(context.getSimpleParamAssignments()+Formatter.endline());

        return predCoreBlock;
    }

    private StringBuilder getDerivativePredStatement(PkMacroDetails pkMacroDetails){

        StringBuilder derivativePredblock = new StringBuilder();
        derivativePredblock.append(getModelStatement());
        derivativePredblock.append(getAbbreviatedStatement());
        derivativePredblock.append(getPKStatement());

        if(pkMacroDetails!=null && !pkMacroDetails.isEmpty()){
            derivativePredblock.append(getPkMacroEquations(pkMacroDetails));
        }
        derivativePredblock.append(getDifferentialInitialConditions());

        DiffEquationStatementBuilder desBuilder = getDiffEquationStatement(derivativePredblock);

        //TODO: getAESStatement();
        derivativePredblock.append(Formatter.endline()+Formatter.error());
        derivativePredblock.append(getErrorStatement(desBuilder));

        return derivativePredblock;

    }

    private StringBuilder getAbbreviatedStatement() {
        StringBuilder abbrStatement = new StringBuilder();
        abbrStatement.append(Formatter.endline());
        int prevBlockValue = 0;

        for(OmegaBlock omegaBlock : context.getCorrelationHandler().getOmegaBlocksInIOV()){
            abbrStatement.append(Formatter.endline());
            Set<Eta> etas =  omegaBlock.getOrderedEtas();
            int iovEtasCount = etas.size();
            boolean isFirstBlock = (prevBlockValue == 0);

            for(Eta iovEta :etas){
                int etaOrder = (isFirstBlock)?iovEta.getOrder():++prevBlockValue;

                StringBuilder etaValues = new StringBuilder();
                prevBlockValue = getIovEtaValueForAbbr(iovEtasCount, etaOrder, etaValues);

                String nextAbbr = Formatter.abbr()+"REPLACE "+Formatter.etaFor(iovEta.getEtaOrderSymbol());
                abbrStatement.append(Formatter.endline(nextAbbr+"="+Formatter.etaFor(etaValues.toString())));
            }
        }
        return abbrStatement;
    }

    //(<eta_order>, 1*<no_of_etas>+<eta_order>, 2*<no_of_etas>+<eta_order>, ... , <no_of_unique_occ_values>*<no_of_etas>+<eta_order>);
    private int getIovEtaValueForAbbr(int iovEtasCount, int etaOrder, StringBuilder etaValues) {
        int etaVal = 0;
        int uniqueOccValuesCount = context.getIovHandler().getIovColumnUniqueValues().size();
        for(int i=0;i<uniqueOccValuesCount;i++){
            etaVal = i*iovEtasCount+etaOrder;
            etaValues.append(etaVal);
            if(i != uniqueOccValuesCount-1){
                etaValues.append(", ");
            }
        }
        return etaVal;
    }

    private String getConditionalDoseDetails() {
        List<ConditionalDoseEvent> conditionalDoseEvents = ScriptDefinitionAccessor.getAllConditionalDoseEvents(context.getScriptDefinition());
        List<TemporalDoseEvent> teDoseEvents = ScriptDefinitionAccessor.getAllTemporalDoseEvent(context.getScriptDefinition());

        StringBuilder doseEvents = new StringBuilder();
        for(ConditionalDoseEvent event : conditionalDoseEvents){
            String statement = context.getConditionalEventHandler().parseConditionalDoseEvent(event);
            if(!StringUtils.isEmpty(statement)){
                doseEvents.append(statement);
            }
        }

        if(!teDoseEvents.isEmpty()){
            for(TemporalDoseEvent event : teDoseEvents){
                String statement = context.getConditionalEventHandler().parseTemporalDoseEvent(event);
                if(!StringUtils.isEmpty(statement)){
                    doseEvents.append(statement);
                }
            }
        }
        return doseEvents.toString();
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

    /**
     * gets pk block for pred statement
     */
    private StringBuilder getPKStatement() {
        StringBuilder pkStatementBlock = new StringBuilder();
        pkStatementBlock.append(Formatter.endline());
        pkStatementBlock.append(Formatter.pk());
        pkStatementBlock.append(getPredCoreStatement());
        pkStatementBlock.append(getAllIndividualParamAssignments());
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
        if(!context.getScriptDefinition().getStructuralBlocks().isEmpty()){
            InitConditionBuilder initBuilder = new InitConditionBuilder();
            builder = initBuilder.getDifferentialInitialConditions(context.getScriptDefinition().getStructuralBlocks());
        }
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
