/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.ConditionalDoseEvent;
import crx.converter.tree.BinaryTree;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.Piece;

/**
 * Conditional event builder helps to build conditional events 
 * for column mapping as well as for multiple dv mapping.
 */
public class ConditionalEventBuilder {
    private final ConversionContext context;

    public ConditionalEventBuilder(ConversionContext context){
        this.context = context; 
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

        String conditionStatement = getConditionStatement(event);
        if(event.getCondition()!=null){
            String condition = parseCondition(event.getCondition(), event.getSource());
            if(!condition.isEmpty()){
                conditionStatement = buildConditionalStatement(condition, conditionStatement);
            }
        }
        eventBuilder.append(conditionStatement);
        return eventBuilder.toString();
    }

    private String getConditionStatement(ConditionalDoseEvent event) {
        BinaryTree assignmentBinaryTree = context.getLexer().getTreeMaker().newInstance(event.getAssignment());
        String assignment = context.getParser().parse(event.getAssignment(), assignmentBinaryTree);

        String conditionStatement = assignment +" = "+ event.getColumnName();
        return conditionStatement;
    }

    private String buildConditionalStatement(String condition, String conditionStatement){
        String format = ("%s")
                +Formatter.endline(Formatter.indent("%s"))
                +Formatter.endline(Formatter.Operator.ENDIF.toString());
        return String.format(format, condition,conditionStatement);
    }

    private String parseCondition(Condition condition, Piece piece){
        BinaryTree bt = context.getLexer().getTreeMaker().newInstance(condition);
        String parsedCondition = context.getParser().parse(piece, bt);

        String format = Formatter.endline()+Formatter.endline("%s");
        String result = new String();
        if(!StringUtils.isEmpty(parsedCondition)){
            parsedCondition = parsedCondition.replaceAll("\\s+","");
            result = Formatter.Operator.IF+"("+parsedCondition+") "+Formatter.Operator.THEN;
        }

        return String.format(format, result);
    }

}
