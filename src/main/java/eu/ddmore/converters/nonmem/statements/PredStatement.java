/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacroList;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class PredStatement {

    private final ConversionContext context;
    private List<PKMacroList> allPkMacros = new ArrayList<PKMacroList>();

    public PredStatement(ConversionContext context){
        this.context = context;
    }
    
    public StringBuilder getPredStatement(){
        String statementName = Formatter.endline()+Formatter.pred();
        allPkMacros = getAllPkMacroLists(context.getScriptDefinition().getStructuralBlocks());
        StringBuilder predStatement = new StringBuilder();
        
        if(!context.getDerivativeVars().isEmpty()){
            //TODO: Add $SUB block. need to have details around it.
            statementName = Formatter.endline()+Formatter.sub();
            predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
            predStatement.append(getDerivativePredStatement().toString());
        }else if(!allPkMacros.isEmpty()){
            //PK macros
            PkMacroAnalyser analyser = new PkMacroAnalyser(allPkMacros);
            String advanType = analyser.getMacroAdvanType();
            predStatement.append(Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+" TRANS=1"));

            predStatement.append(getPKStatement());
            predStatement.append(getErrorStatement());

        }else{
            //non derivative pred block
            predStatement.append(statementName);
            predStatement.append(getNonDerivativePredStatement().toString());
        }
        return predStatement;
    }

    /**
     * Returns non derivative pred statement.
     * @return 
     * 
     */
    //TODO : CHANGE IT.. Talk to Henrik and update how it should work.
    private StringBuilder getNonDerivativePredStatement() {
        StringBuilder sb = new StringBuilder();
        //NM_D is for DOSE
        sb.append(Formatter.endline());
        sb.append(Formatter.endline("IF (AMT.GT.0) NM_D=AMT"));
        sb.append(getPredCoreStatement());
        sb.append(getErrorStatement());

        return sb;
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
        errorBlock.append(Formatter.endline());
        errorBlock.append(Formatter.error());
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
        return pkStatementBlock;
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
     * Collects all pk macro lists from structural blocks in order to create model statement.
     * 
     * @return
     */
    private List<PKMacroList> getAllPkMacroLists(List<StructuralBlock> structuralBlocks) {
        List<PKMacroList> pkMacroLists = new ArrayList<PKMacroList>();
        //        for(StructuralBlock structuralBlock : structuralBlocks){
        //            pkMacroLists.add(structuralBlock.getPkMacrosList());
        //        }
        return pkMacroLists;
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
