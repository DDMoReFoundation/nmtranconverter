/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.common.ConditionalDoseEvent;
import crx.converter.engine.common.MultipleDvRef;
import crx.converter.engine.common.TemporalDoseEvent;
import crx.converter.tree.BinaryTree;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.Piece;

/**
 * Conditional event builder helps to build conditional events 
 * for column mapping as well as for multiple dv mapping.
 */
public class ConditionalEventHandler {
    private final ConversionContext context;

    public ConditionalEventHandler(ConversionContext context){
        this.context = context; 
    }

    /**
     * This method parses the conditions event for dose column mapping when column type is idv/time.
     * 
     * @param event
     * @return
     */
    public String parseTemporalDoseEvent(TemporalDoseEvent event){
        Preconditions.checkNotNull(event, "conditional dose event cannot be null.");
        StringBuilder eventBuilder = new StringBuilder();

        String conditionStatement = getConditionStatement(event.getAssignment(),event.getColumnName());
        if(StringUtils.isEmpty(conditionStatement)){
            return eventBuilder.toString();
        }
        if(event.getCondition()!=null){
            String condition = parseCondition(event.getCondition(), event.getSource());
            if(!condition.isEmpty()){
                conditionStatement = buildConditionalStatement(condition, conditionStatement);
            }
        }
        eventBuilder.append(conditionStatement);
        return eventBuilder.toString();
    }

    /**
     * This method parses the conditions event for dose column mapping.
     * 
     * @param event
     * @return
     */
    public String parseConditionalDoseEvent(ConditionalDoseEvent event) {
        Preconditions.checkNotNull(event, "conditional dose event cannot be null.");
        StringBuilder eventBuilder = new StringBuilder();

        String conditionStatement = getConditionStatement(event.getAssignment(),event.getColumnName());
        if(StringUtils.isEmpty(conditionStatement)){
            return eventBuilder.toString();
        }
        if(event.getCondition()!=null){
            String condition = parseCondition(event.getCondition(), event.getSource());
            if(!condition.isEmpty()){
                conditionStatement = buildConditionalStatement(condition, conditionStatement);
            }
        }
        eventBuilder.append(conditionStatement);
        return eventBuilder.toString();
    }

    /**
     * This method parses the conditions event for dose column mapping.
     * 
     * @param event
     * @return
     */
    public String getMultipleDvCondition(MultipleDvRef dvReference) {
        Preconditions.checkNotNull(dvReference, "conditional dose event cannot be null.");
        StringBuilder eventBuilder = new StringBuilder();

        String conditionStatement = "";
        if(dvReference.getCondition()!=null){
            String condition = parseCondition(dvReference.getCondition(), dvReference.getSource());
            if(!condition.isEmpty()){
                conditionStatement = buildConditionalStatement(condition, conditionStatement);
            }
        }
        eventBuilder.append(conditionStatement);
        return eventBuilder.toString();
    }

    /**
     * Builds simple conditional statement for the condition and statement provided.
     * If conditionStatement is not provided then only condition is returned as 
     * it might be used for customised conditional statements.
     * 
     * @param condition
     * @param conditionStatement
     * @return conditional statement
     */
    public String buildConditionalStatement(String condition, String conditionStatement){
        Preconditions.checkNotNull(condition, "condition cannot be null.");
        if(!StringUtils.isEmpty(conditionStatement)){
            final String endIfStatement = Formatter.endline(Formatter.Operator.ENDIF.toString());
            String format = ("%s")
                    +Formatter.endline(Formatter.indent("%s"))
                    +endIfStatement;
            return String.format(format, condition,conditionStatement);
        }else {
            return condition;
        }
    }

    /**
     * Gets the column reference associated to assignment from the multiple DV reference. 
     * 
     * @param dvReference
     * @return
     */
    public SymbolRef getDVColumnReference(MultipleDvRef dvReference) {
        Preconditions.checkNotNull(dvReference, "multiple dv reference cannot be null.");
        Preconditions.checkNotNull(dvReference.getSource(), "Piece from DV reference cannot be null.");

        SymbolRef columnName = null;
        if (isSymbolReference(dvReference.getSource().getValue())) {
            columnName = (SymbolRef) dvReference.getSource().getValue();
        }
        return columnName;
    }

    private String getConditionStatement(ExpressionValue assignment, String columnName) {
        BinaryTree assignmentBinaryTree = context.getLexer().getTreeMaker().newInstance(assignment);
        String assignmentVal = context.getParser().parse(assignment, assignmentBinaryTree).trim();

        String conditionStatement = "";
        if(context.getDerivativeVarCompSequences().keySet().contains(assignmentVal)){
            return conditionStatement;
        }

        conditionStatement = assignmentVal +" = "+ columnName;
        return conditionStatement;
    }

    private String parseCondition(Condition condition, Piece piece){
        BinaryTree bt = context.getLexer().getTreeMaker().newInstance(condition);
        String parsedCondition = context.getParser().parse(piece, bt);

        String format = Formatter.endline()+Formatter.endline("%s");

        parsedCondition = parsedCondition.replaceAll("\\s+","");
        if(!StringUtils.isEmpty(parsedCondition)){
            return String.format(format, Formatter.Operator.IF+"("+parsedCondition+") "+Formatter.Operator.THEN);
        }else {
            throw new IllegalArgumentException("The condition provided is not valid or it cannot be parsed.");
        }
    }

}
