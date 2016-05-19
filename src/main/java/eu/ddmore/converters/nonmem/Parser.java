/* ----------------------------------------------------------------------------
 * This file is part of the CCoPI-Mono Toolkit. 
 *
 * Copyright (C) 2016 jointly by the following organizations:
 * 1. Mango Solutions, England, UK
 * 2. Cyprotex Discovery Limited, Macclesfield, Cheshire, UK
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution.
 * ----------------------------------------------------------------------------
 */

package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isColumnDefinition;
import static crx.converter.engine.PharmMLTypeChecker.isColumnReference;
import static crx.converter.engine.PharmMLTypeChecker.isConstant;
import static crx.converter.engine.PharmMLTypeChecker.isContinuousCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCorrelation;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCovariateTransform;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionCall;
import static crx.converter.engine.PharmMLTypeChecker.isIndependentVariable;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isInitialCondition;
import static crx.converter.engine.PharmMLTypeChecker.isInt;
import static crx.converter.engine.PharmMLTypeChecker.isJAXBElement;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isLogicalBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isLogicalUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isObservationModel;
import static crx.converter.engine.PharmMLTypeChecker.isParameterEstimate;
import static crx.converter.engine.PharmMLTypeChecker.isPiece;
import static crx.converter.engine.PharmMLTypeChecker.isPiecewise;
import static crx.converter.engine.PharmMLTypeChecker.isPopulationParameter;
import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import static crx.converter.engine.PharmMLTypeChecker.isReal;
import static crx.converter.engine.PharmMLTypeChecker.isRootType;
import static crx.converter.engine.PharmMLTypeChecker.isScalarInterface;
import static crx.converter.engine.PharmMLTypeChecker.isSequence;
import static crx.converter.engine.PharmMLTypeChecker.isString;
import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;
import static crx.converter.engine.PharmMLTypeChecker.isUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isVector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.ConversionDetail_;
import crx.converter.engine.FixedParameter;
import crx.converter.engine.SymbolReader;
import crx.converter.engine.common.BaseParser;
import crx.converter.engine.common.ObservationParameter;
import crx.converter.spi.ILexer;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.commontypes.CategoryRef;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.StringValue;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnReference;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.Constant;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.LogicBinOp;
import eu.ddmore.libpharmml.dom.maths.LogicUniOp;
import eu.ddmore.libpharmml.dom.maths.Piece;
import eu.ddmore.libpharmml.dom.maths.Piecewise;
import eu.ddmore.libpharmml.dom.maths.Uniop;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;

/**
 * NONMEM Expression Parser.
 */
public class Parser extends BaseParser {
    private static final String SEX_COLUMN = "SEX";
    private static final String equationFormat = "%s = %s";	
    private static final String lineFormat = "%s ";
    private HashMap<Object, String> transformation_stmt_map = new HashMap<Object, String>();

    private String doIndividualParameter(IndividualParameter iv) { return z.get(iv); }

    public Parser() throws IOException {
        comment_char = Symbol.COMMENT.toString();
        script_file_suffix = "ctl";
        objective_dataset_file_suffix = "csv";
        output_file_suffix = "csv";
        solver = "ode";
        setReferenceClass(getClass());
        init();

        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("Parser.properties"));
    }



    /**
     * Create a column declaration.
     * Create a column tag like an entry in an NONMEM input line.
     * @param col Column Definition.
     * @return String
     */
    protected String doColumnDefinition(ColumnDefinition col) { return col.getColumnId(); }

    private String doColumnReference(ColumnReference cr) { return cr.getColumnIdRef(); }

    private String doConstant(Constant c) {
        String symbol = unassigned_symbol;

        String op = c.getOp();
        if (op.equalsIgnoreCase("notanumber")) symbol = "NaN";
        else if (op.equalsIgnoreCase("pi")) symbol = "3.1416";
        else if (op.equalsIgnoreCase("exponentiale")) symbol = "EXP(1)";
        else if (op.equalsIgnoreCase("infinity")) symbol = "INF";

        return symbol;
    }

    /**
     * Apply a variable transformation to a covariate.
     * @param transform Transformation definition
     * @return String
     */
    protected String doCovariateTransformation(CovariateTransformation transform) {
        String symbol = unassigned_symbol;

        if (transformation_stmt_map.containsKey(transform)) 
            symbol = transformation_stmt_map.get(transform);
        else {
            BinaryTree bt = lexer.getStatement(transform);
            if (bt == null) throw new NullPointerException("CovariateTransformation AST is NULL");
            symbol = parse(transform, bt);
            transformation_stmt_map.put(transform, symbol);
        }

        return symbol;
    }

    /**
     * Generate code for referencing a derivative term.
     * @param s Derivative
     * @return String
     */
    protected String doDerivative(DerivativeVariable s) { return String.format(s.getSymbId()); }

    private String doIndependentVariable(IndependentVariable v) {
        String symbol = v.getSymbId().toUpperCase();
        symbol = getFormattedSymbol(symbol);
        return symbol;
    }

    private String doInt(IntValue i) { return i.getValue().toString(); }

    /**
     * Write code for local variable reference.
     * @param v Local Variable
     * @return String
     */
    private String doLocalVariable(VariableDefinition v) { return z.get(v.getSymbId()); }

    /**
     * Get the logical operator symbol.
     * @param l_b_op Operator
     * @return String
     */
    protected String doLogicalBinaryOperator(LogicBinOp l_b_op) {
        return getLogicalOperator(l_b_op.getOp());
    }

    /**
     * Get the logical operator symbol.
     * @param u_b_op Operator
     * @return String
     */
    protected String doLogicalUnaryOperator(LogicUniOp u_b_op) {
        return getLogicalOperator(u_b_op.getOp());
    }

    /**
     * Generate code for a parameter reference. 
     * Currently override this method to get access to simple parameter symbol.
     * 
     * @param p Parameter
     * @return String
     */
    private String doParameter(PopulationParameter p) { return z.get(p); }

    private String doPiecewise(Piecewise pw) {
        String symbol = unassigned_symbol;

        List<Piece> pieces = pw.getListOfPiece();
        Piece else_block = null;
        BinaryTree [] assignment_trees = new BinaryTree[pieces.size()]; 
        BinaryTree [] conditional_trees = new BinaryTree[pieces.size()];
        String [] conditional_stmts = new String [pieces.size()];
        String [] assignment_stmts = new String [pieces.size()];

        int assignmentCount = 0, else_index = -1;
        for(int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece != null) {
                // Logical blocks
                Condition cond = piece.getCondition();
                if (cond != null) {
                    conditional_trees[i] = lexer.getTreeMaker().newInstance(piece.getCondition());
                    if (cond.getOtherwise() != null) {
                        else_block = piece;
                        else_index = i;
                    }
                }

                // Assignment block
                BinaryTree assignmentTree = getAssignmentTree(piece.getValue());

                if (assignmentTree != null) {
                    assignmentCount++;
                    assignment_trees[i] = assignmentTree;
                }
            }
        }

        if (assignmentCount == 0) throw new IllegalStateException("A piecewise block has no assignment statements.");

        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);

            if (conditional_trees[i] != null && assignment_trees[i] != null) {
                // Logical condition
                if (!piece.equals(else_block)) conditional_stmts[i] = parse(piece, conditional_trees[i]);

                // Assignment block
                assignment_stmts[i] = parse(new Object(), assignment_trees[i]);
            }
        }

        StringBuilder ifBlock = new StringBuilder();
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);

            if (piece == null) continue;
            else if (piece.equals(else_block)) continue;
            if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) continue;

            String format = Formatter.endline("%s (%s) "+Formatter.Operator.THEN)
                    +Formatter.endline(Formatter.indent("%s = %s"));

            String conditionStatement = getConditionStatement(conditional_stmts[i]);
            ifBlock.append(String.format(format, Formatter.Operator.IF, conditionStatement, field_tag, assignment_stmts[i]));
            ifBlock.append(Formatter.endline(Formatter.Operator.ENDIF.toString()));
        }

        //ELSE block content is defined before IF block instead of ELSE block.
        if (else_block != null && else_index >= 0) {
            String elseformat = Formatter.endline(Formatter.endline()+("%s = %s"));
            block.append(String.format(elseformat, field_tag, assignment_stmts[else_index]));
        }

        if (assignmentCount == 0) throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");

        block.append(ifBlock+Formatter.endline());
        symbol = block.toString();

        return symbol;
    }

    private String doReal(RealValue r) { return Double.toString(r.getValue()); }

    private String doString(String v) { return v; }

    private String doStringValue(StringValue sv) {
        String format = "'%s'";
        return String.format(format, sv.getValue());
    }

    private String doSymbolRef(SymbolRef symbRefType) {
        String symbol = Formatter.getFormattedSymbol(symbRefType.getSymbIdRef()).toUpperCase();
        symbol = replaceIfReservedVarible(symbol);

        return symbol;
    }

    private BinaryTree getAssignmentTree(ExpressionValue expressionValue){
        BinaryTree assignmentTree = null;

        if (isBinaryOperation(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance((Binop) expressionValue);
        }else if (isUnaryOperation(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance((Uniop) expressionValue);
        }else if (isFunctionCall(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance((FunctionCallType) expressionValue);
        } else if (isJAXBElement(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance((JAXBElement<?>) expressionValue);
        } else if (isScalarInterface(expressionValue) || isSymbolReference(expressionValue) || isConstant(expressionValue) ) {
            assignmentTree = lexer.getTreeMaker().newInstance(expressionValue);
        } else 
            throw new IllegalStateException("Piecewise assignment failed (expr='" + expressionValue + "')");
        return assignmentTree;
    }

    private String getCategoryMappingFor(String modelSymbol){
        CategoryRef cref = (CategoryRef) lexer.getAccessor().fetchElement(modelSymbol.toLowerCase());
        if(cref != null){
            modelSymbol = cref.getId();
        }
        return modelSymbol;
    }

    private String getConditionStatement(String conditionStatement) {
        String condition = conditionStatement.replaceAll("\\s+","").toUpperCase();

        if(condition.contains(SEX_COLUMN)){
            String[] conditionWords = condition.split("\\.");
            StringBuilder conditionblock = new StringBuilder();

            for(int count=0;count<conditionWords.length;count++){
                String word = conditionWords[count];
                if(word.equals(SEX_COLUMN)){
                    String comparisonParam = conditionWords[count+2];
                    String comparisonParamVal = getCategoryMappingFor(comparisonParam);
                    conditionWords[count+2] = comparisonParamVal; 
                }
                conditionblock.append(conditionWords[count]);
                conditionblock.append((count!=conditionWords.length-1)?".":"");
            }
            condition = conditionblock.toString();
        }
        return condition;
    }

    /**
     * Return the formatted symbol and also gets appropriate time symbol if its TIME symbol.
     * 
     * @param symbol 
     * @return String formatted symbol
     */
    private String getFormattedSymbol(String symbol) {
        return Formatter.getFormattedSymbol(symbol);
    }

    /**
     * Overrides base parser method to identify binary operator for nmTran conversion.
     * 
     * @param symbol the binary operator symbol from pharmML
     * @return operator the nmtran specific binary operator symbol
     */
    protected String getScriptBinaryOperator(String symbol) {
        return Formatter.propertyHandler.getBinopPropertyFor(symbol);
    }

    @Override
    public String getSymbol(Object o) {
        String symbol = unassigned_symbol;

        if (isSymbolReference(o)) symbol = doSymbolRef((SymbolRef) o); 
        else if (isDerivative(o)) symbol = doDerivative((DerivativeVariable) o);
        else if (isPopulationParameter(o)) symbol = doParameter((PopulationParameter) o);
        else if (isLocalVariable(o)) symbol = doLocalVariable((VariableDefinition) o);
        else if (isString_(o))  symbol = doString((String) o);
        else if (isReal(o)) symbol = doReal((RealValue) o);
        else if (isString(o)) symbol = doStringValue((StringValue) o);
        else if (isInt(o)) symbol = doInt((IntValue) o);
        else if (isConstant(o)) symbol = doConstant((Constant) o);
        else if (isIndependentVariable(o)) symbol = doIndependentVariable((IndependentVariable) o);	 
        else if (isPiecewise(o)) symbol =  doPiecewise((Piecewise) o);
        else if (isLogicalBinaryOperation(o)) symbol = doLogicalBinaryOperator((LogicBinOp) o);
        else if (isLogicalUnaryOperation(o)) symbol = doLogicalUnaryOperator((LogicUniOp) o); 
        else if (isColumnDefinition(o)) symbol = doColumnDefinition((ColumnDefinition) o);
        else if (isCovariateTransform(o)) symbol = doCovariateTransformation((CovariateTransformation) o);
        else if (isColumnReference(o)) symbol = doColumnReference((ColumnReference) o);
        else if (isIndividualParameter(o)) symbol = doIndividualParameter((IndividualParameter) o);
        else 
        {
            String format = "WARNING: Unknown symbol, %s\n";
            String value = "null";
            if (o != null) value = o.toString();
            String msg = String.format(format, value);
            ConversionDetail detail = new ConversionDetail_();
            detail.setSeverity(ConversionDetail.Severity.WARNING);
            detail.addInfo("warning", msg);

            System.err.println(msg); 
        }

        return symbol;
    }

    @Override
    public SymbolReader getSymbolReader() { return z; }

    private String getValueWhenPiecewise(Node leaf, boolean inPiecewise, String description) {
        if(inPiecewise){
            return String.format(lineFormat,leaf.data);
        }else{
            return Formatter.endline(String.format(equationFormat, description, leaf.data));
        }
    }

    @Override
    public void initialise() {
        // N/B Does nothing, handle for Parser specific initialisation behaviour.
    }

    private String replaceIfReservedVarible(String variable) {
        String varSymbol = variable.toUpperCase();
        if (lexer.isFilterReservedWords()) {
            if (getSymbolReader().isReservedWord(varSymbol)) {
                varSymbol = getSymbolReader().replacement4ReservedWord(varSymbol);
                if (varSymbol == null){
                    throw new NullPointerException("Replacement symbol for reserved word ('" + varSymbol + "') undefined.");
                }
            }
        }
        return varSymbol;
    }

    protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
        if (leaf == null) throw new NullPointerException("Tree leaf is NULL.");

        if (leaf.data != null) {
            boolean inPiecewise = (isPiecewise(leaf.data));

            if (!isString_(leaf.data)) leaf.data = getSymbol(leaf.data);
            String current_value = "", current_symbol = "";
            if(isLocalVariable(context)){
                current_symbol = getSymbol(context).toUpperCase();
                current_value = getValueWhenPiecewise(leaf, inPiecewise, current_symbol);
            }else if (isDerivative(context) || isPopulationParameter(context)) {
                current_symbol = getSymbol(context);
                String description = (isRootType(context))? readDescription((PharmMLRootType) context) : current_symbol;
                current_value = getValueWhenPiecewise(leaf, inPiecewise, description);
            } else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
                String format = "(%s) ";
                current_value = String.format(format, (String) leaf.data);
            } else if (isPiece(context)) {
                current_value = String.format(lineFormat, (String) leaf.data);
            } else if (isContinuousCovariate(context)) {
                current_symbol = getSymbol(context).toUpperCase();
                current_value = Formatter.endline(String.format(equationFormat, current_symbol, leaf.data));
                //} else if (isActivity(context)) {
                //    current_value = getSymbol(new ActivityDoseAmountBlock((Activity) context, (String) leaf.data));
            } else if (isIndividualParameter(context)) {
                current_symbol = getSymbol(context);
                current_value = getValueWhenPiecewise(leaf, inPiecewise, current_symbol);
            } else if (isRandomVariable(context)) {
                ParameterRandomVariable rv = (ParameterRandomVariable) context;
                current_value = Formatter.endline(String.format(equationFormat, rv.getSymbId(), (String) leaf.data));
            } else if (isCovariate(context)) {
                CovariateDefinition cov = (CovariateDefinition) context;
                current_value = Formatter.endline(String.format(equationFormat, cov.getSymbId(), (String) leaf.data));
            } else if(isCovariateTransform(context)){
                CovariateTransformation cov = (CovariateTransformation) context;
                current_symbol = cov.getTransformedCovariate().getSymbId();
                current_value = getValueWhenPiecewise(leaf, inPiecewise, current_symbol);
            }
            else if (isObservationModel(context)) {
                ObservationParameter op = (ObservationParameter) context;
                current_value = Formatter.endline(String.format(equationFormat, op.getName(), (String) leaf.data));
            } else if (isCorrelation(context)) {
                current_value = (String) leaf.data;
            } else if (isObservationModel(context)) {
                current_value = Formatter.endline(((String) leaf.data).trim()) + Formatter.endline();
            } else if (isParameterEstimate(context) || context instanceof FixedParameter) 
            {
                current_value = (String) leaf.data;
            } else {
                current_value = String.format(lineFormat, (String) leaf.data);
            } 

            if (current_value != null) {
                if(inPiecewise) {
                    if (current_symbol != null) current_value = Formatter.endline(current_value.replaceAll(field_tag, current_symbol));
                }
                fout.write(current_value);
            }
        } else
            throw new IllegalStateException("Should be a statement string bound to the root.data element.");
    }

    @Override
    public void setLexer(ILexer lexer_) {
        if (lexer_ == null) throw new NullPointerException("The lexer is NULL.");
        lexer = lexer_;
        z = new SymbolReader(lexer);
    }

    @Override
    public void writePreMainBlockElements(PrintWriter fout, File src) throws IOException{
        String model_filename = src.getName().replace(".xml", "");
        writeScriptHeader(fout, model_filename);
    }

    @Override
    protected void writeScriptHeader(PrintWriter fout, String model_file) throws IOException {
        if (fout == null) return;
        String format = "%s Script generated by the pharmML2Nmtran Converter v."+lexer.getConverterVersion();
        fout.write(Formatter.endline(String.format(format, comment_char)));
        format = "%s Source"+Formatter.indent(": %s");
        fout.write(Formatter.endline(String.format(format, comment_char, lexer.getSource())));
        format = "%s Target"+Formatter.indent(": %s");
        fout.write(Formatter.endline(String.format(format, comment_char, lexer.getTarget())));
        format = "%s Model "+Formatter.indent(": %s");
        fout.write(Formatter.endline(String.format(format, comment_char, model_file)));
        format = "%s Dated "+Formatter.indent(Formatter.endline(": %s"));
        fout.write(Formatter.endline(String.format(format, comment_char, new Date())));
    }
}
