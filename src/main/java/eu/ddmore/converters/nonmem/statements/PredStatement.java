/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;
import java.util.Map;

import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 *
 */
public class PredStatement {

    private final ConversionContext context;

    public PredStatement(ConversionContext context){
        this.context = context;
    }

    public StringBuilder getPredStatement(){
        String statementName = Formatter.endline()+Formatter.pred();
        StringBuilder predStatement = new StringBuilder();
        PkMacroAnalyser analyser = new PkMacroAnalyser();
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
     * 
     */
    //TODO : CHANGE IT.. Talk to Henrik and update how it should work.
    private StringBuilder getNonDerivativePredStatement() {
        StringBuilder nonDerivativePredBlock = new StringBuilder();
        //NM_D is for DOSE
        nonDerivativePredBlock.append(Formatter.endline("IF (AMT.GT.0) NM_D=AMT"));
        nonDerivativePredBlock.append(getPredCoreStatement());
        nonDerivativePredBlock.append(getErrorStatement());

        return nonDerivativePredBlock;
    }

    /**
     * gets pred core statement for nonmem file.
     */
    private StringBuilder getPredCoreStatement() {
        StringBuilder predCoreBlock = new StringBuilder();
        List<ParameterBlock> blocks = context.getScriptDefinition().getParameterBlocks();
        predCoreBlock.append(Formatter.endline(context.buildThetaAssignments().toString()));
        predCoreBlock.append(Formatter.endline(context.buildEtaAssignments().toString()));
        predCoreBlock.append(getAllIndividualParamAssignments(blocks));
        return predCoreBlock;
    }

    private StringBuilder getDerivativePredStatement() {
        StringBuilder DerivativePredblock = new StringBuilder();
        DerivativePredblock.append(getModelStatement());
        //TODO : getAbbreviatedStatement();
        DerivativePredblock.append(getPKStatement());
        Formatter.setInDesBlock(true);
        DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(context);
        DerivativePredblock.append(desBuilder.getDifferentialEquationsStatement());
        Formatter.setInDesBlock(false);
        //TODO: getAESStatement();
        DerivativePredblock.append(Formatter.endline()+Formatter.error());
        DerivativePredblock.append(getErrorStatement(desBuilder.getDefinitionsParsingMap()));

        return DerivativePredblock;
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
     * get Error statement for nonmem pred block
     * This block will rename function name if it is already defined in DES and also redefine it in ERROR block.
     * @return 
     * 
     */
    private String getErrorStatement(Map<String, String> definitionsParsingMap) {
        StringBuilder errorBlock = new StringBuilder();
        for(ErrorStatement errorStatement: context.getErrorStatements()){
            if(definitionsParsingMap != null){
                errorBlock.append(errorStatement.getDetailsForDES(definitionsParsingMap,context.getDerivativeVarCompSequences()));
            }else{
                errorBlock.append(errorStatement.getErrorStatementDetails());
            }
        }
        return errorBlock.toString();
    }

    /**
     * gets pk block for pred statement
     */
    private StringBuilder getPKStatement() {
        StringBuilder pkStatementBlock = new StringBuilder();
        pkStatementBlock.append(Formatter.endline());
        pkStatementBlock.append(Formatter.pk());
        pkStatementBlock.append(getPredCoreStatement());
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
    public StringBuilder getDifferentialInitialConditions(){
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
    public StringBuilder getAllIndividualParamAssignments(List<ParameterBlock> blocks) {
        StringBuilder IndividualParamAssignmentBlock = new StringBuilder();
        IndividualDefinitionEmitter individualDefEmitter = new IndividualDefinitionEmitter(context);
        for(ParameterBlock parameterBlock : blocks){
            for(IndividualParameter parameterType: parameterBlock.getIndividualParameters()){
                IndividualParamAssignmentBlock.append(individualDefEmitter.createIndividualDefinition(parameterType));
            }
        }
        return IndividualParamAssignmentBlock;
    }
}
