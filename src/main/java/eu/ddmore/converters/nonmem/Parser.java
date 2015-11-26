/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isActivity;
import static crx.converter.engine.PharmMLTypeChecker.isBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isConstant;
import static crx.converter.engine.PharmMLTypeChecker.isContinuousCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCorrelation;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCovariateTransform;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionCall;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isInitialCondition;
import static crx.converter.engine.PharmMLTypeChecker.isJAXBElement;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isObservationModel;
import static crx.converter.engine.PharmMLTypeChecker.isParameter;
import static crx.converter.engine.PharmMLTypeChecker.isParameterEstimate;
import static crx.converter.engine.PharmMLTypeChecker.isPiece;
import static crx.converter.engine.PharmMLTypeChecker.isPiecewise;
import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import static crx.converter.engine.PharmMLTypeChecker.isRootType;
import static crx.converter.engine.PharmMLTypeChecker.isScalarInterface;
import static crx.converter.engine.PharmMLTypeChecker.isSequence;
import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;
import static crx.converter.engine.PharmMLTypeChecker.isVector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.BaseParser;
import crx.converter.engine.CategoryRef;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ObservationBlock.ObservationParameter;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.Constant;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.Piece;
import eu.ddmore.libpharmml.dom.maths.Piecewise;
import eu.ddmore.libpharmml.dom.maths.Uniop;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameter;
import eu.ddmore.libpharmml.dom.trialdesign.Activity;

public class Parser extends BaseParser {

    private static final String SEX_COLUMN = "SEX";
    private static final String equationFormat = "%s = %s";
    private static final String lineFormat = "%s ";

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

    @Override
    protected String doSymbolRef(SymbolRef symbRefType) {

        String symbol = Formatter.getFormattedSymbol(symbRefType.getSymbIdRef()).toUpperCase();
        symbol = replaceIfReservedVarible(symbol);

        return symbol;
    }

    private String getCategoryMappingFor(String modelSymbol){
        CategoryRef cref = (CategoryRef) lexer.getAccessor().fetchElement(modelSymbol.toLowerCase());
        if(cref != null){
            modelSymbol = cref.getDataSymbol();
        }
        return modelSymbol;
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
    @Override
    protected String getScriptBinaryOperator(String symbol) {
        return Formatter.propertyHandler.getBinopPropertyFor(symbol);
    }

    /**
     * Generate code for a parameter reference. 
     * Currently override this method to get access to simple parameter symbol.
     * 
     * @param p Parameter
     * @return String
     */
    @Override
    protected String doParameter(SimpleParameter p) {
        return z.get(p);
    }

    @Override
    protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
        if (leaf == null) throw new NullPointerException("Tree leaf is NULL.");

        if (leaf.data != null) {
            boolean inPiecewise = (isPiecewise(leaf.data));

            if (!isString_(leaf.data)) leaf.data = getSymbol(leaf.data);
            String current_value = "", current_symbol = "";
            if(isLocalVariable(context)){
                current_symbol = getSymbol(context).toUpperCase();
                current_value = getValueWhenPiecewise(leaf, inPiecewise, current_symbol);
            }else if (isDerivative(context) || isParameter(context)) {
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
            } else if (isActivity(context)) {
                current_value = getSymbol(new ActivityDoseAmountBlock((Activity) context, (String) leaf.data));
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

    private String getValueWhenPiecewise(Node leaf, boolean inPiecewise, String description) {
        if(inPiecewise){
            return String.format(lineFormat,leaf.data);
        }else{
            return Formatter.endline(String.format(equationFormat, description, leaf.data));
        }
    }

    /*
     * (non-Javadoc)
     * @see crx.converter.engine.BaseParser#doIndependentVariable(eu.ddmore.libpharmml.dom.IndependentVariable)
     */
    @Override
    protected String doIndependentVariable(IndependentVariable v) {
        String symbol = v.getSymbId().toUpperCase();
        symbol = getFormattedSymbol(symbol);
        return symbol;
    }

    @Override
    protected String doConstant(Constant c) {
        String symbol = unassigned_symbol;

        String op = c.getOp();
        if (op.equalsIgnoreCase("notanumber")) symbol = "NaN";
        else if (op.equalsIgnoreCase("pi")) symbol = "3.1416";
        else if (op.equalsIgnoreCase("exponentiale")) symbol = "EXP(1)";
        else if (op.equalsIgnoreCase("infinity")) symbol = "INF";

        return symbol;
    }

    @Override
    protected String doPiecewise(Piecewise pw) {
        String symbol = unassigned_symbol;

        List<Piece> pieces = pw.getPiece();
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

    private BinaryTree getAssignmentTree(ExpressionValue expressionValue){
        BinaryTree assignmentTree = null;

        if (isBinaryOperation(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((Binop) expressionValue));
        }else if (isUnaryOperation(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((Uniop) expressionValue));
        }else if (isFunctionCall(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((FunctionCallType) expressionValue));
        } else if (isJAXBElement(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((JAXBElement<?>) expressionValue));
        } else if (isScalarInterface(expressionValue) || isSymbolReference(expressionValue) || isConstant(expressionValue) ) {
            assignmentTree = lexer.getTreeMaker().newInstance(expressionValue);
        } else 
            throw new IllegalStateException("Piecewise assignment failed (expr='" + expressionValue + "')");
        return assignmentTree;
    }

    @Override
    public String getScriptFilename(String output_dir) {
        String format = "%s/%s.%s";
        return String.format(format, output_dir, run_id, script_file_suffix);
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
