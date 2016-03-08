/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * This class contains helper methods for model statement and associated statement blocks which are part of it.
 */
public class ModelStatementHelper {

    private final ConversionContext context;
    private ErrorStatementHandler errorStatementHandler;
    private PredCoreStatement predCoreStatement;

    public ModelStatementHelper(ConversionContext context){
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context= context;
        initialise();
    }

    private void initialise(){
        errorStatementHandler = new ErrorStatementHandler(context);
        predCoreStatement = new PredCoreStatement(context);
    }

    /**
     * This method will collect all the parsing for Individual parameter assignments.
     *  
     * @param blocks
     * @return
     */
    public StringBuilder getAllIndividualParamAssignments() {
        StringBuilder individualParamAssignmentBlock = new StringBuilder();
        IndividualDefinitionEmitter individualDefEmitter = new IndividualDefinitionEmitter(context);
        for(ParameterBlock parameterBlock : context.getScriptDefinition().getParameterBlocks()){
            for(IndividualParameter parameterType: parameterBlock.getIndividualParameters()){
                individualParamAssignmentBlock.append(individualDefEmitter.createIndividualDefinition(parameterType));
            }
        }
        return individualParamAssignmentBlock;
    }

    /**
     * Gets differential equation statement with help of diff equation builder
     * 
     * @param statement
     * @return diff equation statement
     */
    public DiffEquationStatementBuilder getDiffEquationStatement(StringBuilder statement) {
        statement.append(Formatter.des());

        DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(context);
        Formatter.setInDesBlock(true);
        statement.append(desBuilder.getDifferentialEquationsStatement());
        Formatter.setInDesBlock(false);

        return desBuilder;
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
                    String rhs = context.getLocalParserHelper().parse(definitionType);
                    if(!Formatter.isInDesBlock())
                        varDefinitionsBlock.append(rhs);
                }
            }
        }
        return varDefinitionsBlock;
    }

    public ConversionContext getContext() {
        return context;
    }

    public ErrorStatementHandler getErrorStatementHandler() {
        return errorStatementHandler;
    }

    public PredCoreStatement getPredCoreStatement() {
        return predCoreStatement;
    }
}
