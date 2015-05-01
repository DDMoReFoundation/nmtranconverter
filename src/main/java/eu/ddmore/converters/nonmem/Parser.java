/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isActivity;
import static crx.converter.engine.PharmMLTypeChecker.isBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isConstant;
import static crx.converter.engine.PharmMLTypeChecker.isContinuousCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCorrelation;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.BaseParser;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.ObservationBlock.ObservationParameter;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import eu.ddmore.converters.nonmem.statements.PredStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
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
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelation;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.GeneralCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.trialdesign.Activity;

public class Parser extends BaseParser {

    private ParametersHelper parameters;
    private Properties binopProperties;
    private ArrayList<String> thetas = new ArrayList<String>();

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

        binopProperties = loadBinopProperties();
    }

    /*
     * (non-Javadoc)
     * @see crx.converter.engine.BaseParser#doSymbolRef(eu.ddmore.libpharmml.dom.commontypes.SymbolRef)
     */
    @Override
    protected String doSymbolRef(SymbolRef symbRefType) {

        String symbol = getFormattedSymbol(symbRefType.getSymbIdRef());

        return symbol;
    }

    /**
     * Return the formatted symbol and also gets appropriate time symbol if its TIME symbol.
     * 
     * @param symbol 
     * @return String formatted symbol
     */
    private String getFormattedSymbol(String symbol) {
        if (isTimeSymbol(symbol)){
            symbol = Formatter.getTimeSymbol();
        } else{
            symbol = Formatter.addPrefix(symbol);
        }
        return symbol;
    }

    /**
     * Overrides base parser method to identify binary operator for nmTran conversion.
     * 
     * @param symbol the binary operator symbol from pharmML
     * @return operator the nmtran specific binary operator symbol
     */
    @Override
    protected String getScriptBinaryOperator(String symbol) {
        symbol = symbol.toLowerCase();
        if(binopProperties!=null && binopProperties.stringPropertyNames().contains(symbol)){
            return binopProperties.getProperty(symbol);
        }else{
            throw new IllegalStateException("Binary Operation not recognised : "+ symbol);
        }
    }

    /**
     * Loads binary operator properties from properties file. 
     * @return
     */
    private Properties loadBinopProperties() {
        Properties binopProperties = new Properties();
        try {
            binopProperties.load(Parser.class.getResourceAsStream("binary_operator.properties"));
        } catch (IOException e) {
            throw new IllegalStateException("Binary Operation not recognised : "+ e);
        }
        return binopProperties;
    }

    /*
     * (non-Javadoc)
     * @see crx.converter.engine.BaseParser#rootLeafHandler(java.lang.Object, crx.converter.tree.Node, java.io.PrintWriter)
     */
    @Override
    protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
        if (leaf == null) throw new NullPointerException("Tree leaf is NULL.");

        if (leaf.data != null) {
            boolean inPiecewise = false;
            String equationFormat = "%s = %s";
            String lineFormat = "%s ";
            if (isPiecewise(leaf.data)) inPiecewise = true;

            if (!isString_(leaf.data)) leaf.data = getSymbol(leaf.data);
            String current_value = "", current_symbol = "";
            if(isLocalVariable(context)){
                current_symbol = Formatter.addPrefix(getSymbol(context));
                if(inPiecewise){
                    current_value = String.format(lineFormat,leaf.data);    
                }else{
                    current_value = Formatter.endline(String.format(equationFormat, current_symbol, leaf.data));
                }
            }else if (isDerivative(context) || isParameter(context)) {
                String description = "";
                if (isRootType(context)) description = readDescription((PharmMLRootType) context);
                current_symbol = getSymbol(context);
                current_value = Formatter.endline(String.format(equationFormat, Formatter.addPrefix(description), leaf.data));
            } else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
                String format = "(%s) ";
                current_value = String.format(format, (String) leaf.data);
            } else if (isPiece(context)) { 
                current_value = String.format(lineFormat, (String) leaf.data);
            } else if (isContinuousCovariate(context)) {
                current_symbol = Formatter.addPrefix(getSymbol(context));
                current_value = Formatter.endline(String.format(equationFormat, current_symbol, leaf.data));
            } else if (isActivity(context)) { 
                current_value = getSymbol(new ActivityDoseAmountBlock((Activity) context, (String) leaf.data));
            } else if (isIndividualParameter(context)) { 
                current_value = (String) leaf.data;
            } else if (isRandomVariable(context)) {
                ParameterRandomVariable rv = (ParameterRandomVariable) context;
                current_value = Formatter.endline(String.format(equationFormat, rv.getSymbId(), (String) leaf.data));
            } else if (isCovariate(context)) { 
                CovariateDefinition cov = (CovariateDefinition) context;
                current_value = Formatter.endline(String.format(equationFormat, cov.getSymbId(), (String) leaf.data));
            } else if (isObservationModel(context)) {
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

    /**
     * Checks if current symbol is Time symbol represented as either T or TIME.
     *  
     * @param symbol
     * @return boolean - whether symbol is symbol for time
     */
    private boolean isTimeSymbol(String symbol) {
        return symbol.equals(ColumnConstant.TIME.toString()) || symbol.equals(NmConstant.T.toString());
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

    public String doPiecewise(Piecewise pw) {
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

        int block_assignment = 0;
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece == null) continue;
            else if (piece.equals(else_block)) continue;

            if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) continue;	
            String operator = "IF";
            String format = Formatter.endline("%s (%s) THEN")+Formatter.endline(Formatter.indent("%s = %s"));
            if (block_assignment > 0) {
                operator = "ELSE IF ";
                format = Formatter.endline(" %s (%s)")+Formatter.endline(Formatter.indent("%s = %s"));
            }
            String conditionStatement = conditional_stmts[i].replaceAll("\\s+","");
            block.append(String.format(format, operator, conditionStatement, field_tag, assignment_stmts[i]));
            block_assignment++;
        }

        if (else_block != null && else_index >= 0) {
            block.append(Formatter.endline("ELSE"));
            String format = Formatter.endline(Formatter.indent("%s = %s"));
            block.append(String.format(format, field_tag, assignment_stmts[else_index]));
        }
        block.append("ENDIF");
        if (assignmentCount == 0) throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
        symbol = block.toString();

        return symbol;
    }
    
    private BinaryTree getAssignmentTree(ExpressionValue expressionValue){
        BinaryTree assignmentTree = null;
        
        if (isBinaryOperation(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((Binop) expressionValue));
        } else if (isFunctionCall(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((FunctionCallType) expressionValue));
        } else if (isJAXBElement(expressionValue)) {
            assignmentTree = lexer.getTreeMaker().newInstance(getEquation((JAXBElement<?>) expressionValue));
        } else if (isScalarInterface(expressionValue) || isSymbolReference(expressionValue) || isConstant(expressionValue) ) {
            assignmentTree = lexer.getTreeMaker().newInstance(expressionValue);
        } else 
            throw new IllegalStateException("Piecewise assignment failed (expr='" + expressionValue + "')");
        return assignmentTree;
    }

    Map<String, Integer> etasOrder = new LinkedHashMap<String, Integer>();

    @Override
    public void writeParameterStatement(PrintWriter fout) {
        if (fout == null) return;
        if (lexer.getModelParameters().isEmpty()) {
            return;
        }
        parameters = new ParametersHelper(lexer.getScriptDefinition());
        setEtasOrder(parameters.createOrderedEtasMap());
        parameters.initialiseAllParameters(lexer.getModelParameters());

        setThetaAssigments();
        buildPredStatement(fout);

        StringBuilder thetaStatement = parameters.getThetaStatementBlock();
        fout.write(thetaStatement.toString());

        StringBuilder omegaStatement = parameters.getOmegaStatementBlock();
        fout.write(omegaStatement.toString());

        StringBuilder sigmaStatement = parameters.getSigmaStatementBlock();
        fout.write(sigmaStatement.toString());

    }

    /**
     * Builds and writes pred statement block to file.
     *  
     * @param fout
     */
    public void buildPredStatement(PrintWriter fout){
        PredStatement predStatement = new PredStatement(lexer.getScriptDefinition(),this);
        predStatement.getPredStatement(fout);
    }

    /**
     * Set theta assignements from theta parameters generated.
     */
    private void setThetaAssigments(){
        thetas.addAll(parameters.getThetaParams().keySet());
    }

    /**
     * 
     * @param symbol
     * @return
     */
    public String getThetaForSymbol(String symbol){
        if(thetas.isEmpty()){
            setThetaAssigments();
        }
        if(thetas.contains(symbol)){
            symbol = String.format(Block.THETA+"(%s)",thetas.indexOf(symbol)+1);
        }
        return symbol;
    }

    /**
     * Get individual parameter definition from individual parameter type.
     * This method is used as part of pred core block.
     * 
     * @param ip
     * @return
     */
    public String createIndividualDefinition(IndividualParameter ip){
        StringBuilder statement = new StringBuilder();

        String variableSymbol = ip.getSymbId();    	
        if (ip.getAssign() != null) {
            statement = getIndivDefinitionForAssign(ip);
        } 
        else if (ip.getGaussianModel() != null) {

            GaussianModel gaussianModel = ip.getGaussianModel();
            String logType = getLogType(gaussianModel.getTransformation());
            String pop_param_symbol = parameters.getPopSymbol(gaussianModel);
            variableSymbol = (pop_param_symbol.isEmpty())?variableSymbol:parameters.getMUSymbol(pop_param_symbol);

            statement.append(String.format("%s = ", variableSymbol));

            if(gaussianModel.getLinearCovariate()!=null){
                if (!pop_param_symbol.isEmpty()) {
                    statement.append(String.format(logType+"(%s)", Formatter.addPrefix(pop_param_symbol)));
                }

                List<CovariateRelation> covariates = gaussianModel.getLinearCovariate().getCovariate();
                if (covariates != null) {
                    for (CovariateRelation covariate : covariates) {
                        if (covariate == null) continue;
                        PharmMLRootType type = lexer.getAccessor().fetchElement(covariate.getSymbRef());
                        statement.append(getCovariateForIndividualDefinition(covariate, type));
                    }
                }
            }
            else if (gaussianModel.getGeneralCovariate() != null) {
                GeneralCovariate generalCov = gaussianModel.getGeneralCovariate(); 
                String assignment = parse(generalCov, lexer.getStatement(generalCov));
                statement.append(assignment);
            }
            statement.append(Formatter.endline(comment_char));

            StringBuilder etas = addEtasStatementsToIndivParamDef(gaussianModel.getRandomEffects());
            if (logType.equals(NmConstant.LOG.toString())) {
                String format = Formatter.endline("%s = EXP(%s %s);");
                statement.append(String.format(format, Formatter.addPrefix(ip.getSymbId()), variableSymbol,etas));
            } else if (logType.equals(NmConstant.LOGIT.toString())) {
                String format = Formatter.endline("%s = 1./(1 + exp(-%s));");
                statement.append(String.format(format, Formatter.addPrefix(ip.getSymbId()), variableSymbol));
            }
        }
        statement.append(Formatter.endline());

        return statement.toString();
    }

    /**
     * This method will retrieve details of parameter type passed depending upon 
     * whether it is IDV or Covariate Definition.
     * 
     * @param covariate
     * @param type
     * @return
     */
    private StringBuilder getCovariateForIndividualDefinition(CovariateRelation covariate, PharmMLRootType type) {
        StringBuilder statement = new StringBuilder();
        if(type instanceof IndependentVariable){
            String idvName = doIndependentVariable((IndependentVariable)type);
            statement.append("+"+idvName);
        }
        else if(type instanceof CovariateDefinition){
            CovariateDefinition covariateDef = (CovariateDefinition) type;
            if (covariateDef != null) {
                if (covariateDef.getContinuous() != null) {
                    String covStatement = "";
                    ContinuousCovariate continuous = covariateDef.getContinuous();
                    if (continuous.getListOfTransformation() != null && !continuous.getListOfTransformation().isEmpty()){
                        covStatement = getSymbol(continuous.getListOfTransformation().get(0));
                    }
                    else covStatement = covariateDef.getSymbId();

                    covStatement = addFixedEffectsStatementToIndivParamDef(covariate, covStatement);
                    if(!covStatement.isEmpty())
                        statement.append("+"+covStatement);
                } else if (covariateDef.getCategorical() != null) {
                    throw new UnsupportedOperationException("No categorical yet");
                }
            }
        }
        return statement;
    }

    /**
     * This method parses and adds fixed effects statements from covariates to individual parameter definition .
     * @param covariate
     * @param covStatement
     * @return
     */
    private String addFixedEffectsStatementToIndivParamDef(CovariateRelation covariate, String covStatement) {
        FixedEffectRelation fixedEffect = covariate.getFixedEffect();
        if (fixedEffect != null) {
            String  fixedEffectStatement = Formatter.addPrefix(fixedEffect.getSymbRef().getSymbIdRef());
            if(fixedEffectStatement.isEmpty())
                fixedEffectStatement = parse(fixedEffect);
            covStatement = fixedEffectStatement + " * " + covStatement;
        }
        return covStatement;
    }

    /**
     * This method will return individual definition details if assignment is present.
     * @param ip
     * @return
     */
    private StringBuilder getIndivDefinitionForAssign(IndividualParameter ip) {
        StringBuilder statement = new StringBuilder();
        if (ip.getAssign() != null) {
            statement.append(Formatter.endline());
            statement.append(String.format("%s = ", ip.getSymbId()));
            String assignment = parse(new Object(), lexer.getStatement(ip.getAssign()));
            statement.append(Formatter.endline(assignment+Symbol.COMMENT));
        }
        return statement;
    }

    /**
     * Return log type in constant format depending on transformation type
     * This method is mainly used to help creation of individual parameter definitions.
     * 
     * @param transform
     * @return
     */
    private String getLogType(LhsTransformation transform) {
        if (transform == LhsTransformation.LOG){
            return NmConstant.LOG.toString();
        }else if (transform == LhsTransformation.LOGIT){
            return NmConstant.LOGIT.toString();
        }else{
            throw new  UnsupportedOperationException("Tranformation type "+transform.name()+" not yet supported");
        }
    }

    /**
     * This method adds etas from random effects to individual parameter definitions.
     * @param random_effects
     * @return
     */
    private StringBuilder addEtasStatementsToIndivParamDef(List<ParameterRandomEffect> random_effects) {
        StringBuilder etas = new StringBuilder();
        if (random_effects != null && !random_effects.isEmpty()) {
            for (ParameterRandomEffect random_effect : random_effects) {
                if (random_effect == null) continue;
                etas.append("+ ");
                etas.append("ETA("+etasOrder.get(random_effect.getSymbRef().get(0).getSymbIdRef())+")");
            }
        }
        return etas;
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

    public void writeScriptHeader(PrintWriter fout, String model_file) throws IOException {
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

    /**
     * Method to be used by other classes which doesnt have visibility to BaseParser
     * @param context
     * @return
     */
    public String parse(Object context){
        return parse(context, lexer.getStatement(context));
    }

    public Map<String, Integer> getEtasOrder() {
        return etasOrder;
    }

    public void setEtasOrder(Map<String, Integer> etasOrder) {
        this.etasOrder = etasOrder;
    }

    public ParametersHelper getParameters() {
        return parameters;
    }
}
