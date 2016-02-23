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


public class LocalParserHelper {

    ConversionContext context;

    public LocalParserHelper(ConversionContext context){
        this.context = context;
    }

    /**
     * It will parse the object passed and returns parsed results in form of string.
     *  
     * @param context
     * @return
     */
    public String parse(Object objContext){
        return parse(objContext, context.getLexer().getStatement(objContext));
    }

    /**
     * It will parse the object passed with help of binary tree provided.
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
        BinaryTree assignmentTree = getAssignmentTree((ExpressionValue) rhs.getContent());
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
