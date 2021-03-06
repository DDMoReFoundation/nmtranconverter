/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isConstant;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionCall;
import static crx.converter.engine.PharmMLTypeChecker.isJAXBElement;
import static crx.converter.engine.PharmMLTypeChecker.isScalarInterface;
import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;
import static crx.converter.engine.PharmMLTypeChecker.isUnaryOperation;

import javax.xml.bind.JAXBElement;

import crx.converter.tree.BinaryTree;
import crx.converter.tree.TreeMaker;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.Uniop;

/**
 * This parser helper is to assist parsing with help of common converter parser.
 */
public class LocalParserHelper {

    ConversionContext context;

    public LocalParserHelper(ConversionContext context){
        this.context = context;
    }

    /**
     * It parses the object passed and returns parsed results in form of string.
     *  
     * @param context
     * @return
     */
    public String parse(Object objContext){
        return parse(objContext, context.getLexer().getStatement(objContext));
    }

    /**
     * It parses the object passed with help of binary tree provided.
     * This tree is generated by common converter for elements.
     * 
     * @param objContext
     * @return
     */
    public String parse(Object objContext, BinaryTree tree){
        return context.getParser().parse(objContext, tree);
    }

    /**
     * Gets values for Rhs which are in form of expression value
     * @param rhs
     * @return
     */
    public String getParsedValueForRhs(Rhs rhs){
        return getParsedValueForExpressionValue((ExpressionValue) rhs.getContent());
    }

    /**
     * Gets parsed values of expression value
     * @param value
     * @return
     */
    public String getParsedValueForExpressionValue(ExpressionValue value){
        BinaryTree assignmentTree = getAssignmentTree(value);
        return context.getParser().parse(new Object(), assignmentTree);
    }

    private BinaryTree getAssignmentTree(ExpressionValue expressionValue){
        BinaryTree assignmentTree = null;
        TreeMaker treeMaker = context.getLexer().getTreeMaker();
        if (isBinaryOperation(expressionValue)) {
            assignmentTree = treeMaker.newInstance((Binop) expressionValue);
        }else if (isUnaryOperation(expressionValue)) {
            assignmentTree = treeMaker.newInstance((Uniop) expressionValue);
        }else if (isFunctionCall(expressionValue)) {
            assignmentTree = treeMaker.newInstance((FunctionCallType) expressionValue);
        } else if (isJAXBElement(expressionValue)) {
            assignmentTree = treeMaker.newInstance((JAXBElement<?>) expressionValue);
        } else if (isScalarInterface(expressionValue) || isSymbolReference(expressionValue) || isConstant(expressionValue) ) {
            assignmentTree = treeMaker.newInstance(expressionValue);
        } else 
            throw new IllegalStateException("Assignment tree failed for (expr='" + expressionValue + "')");
        return assignmentTree;
    }
}
