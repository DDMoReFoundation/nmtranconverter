/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isActivity;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isIndependentVariable;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isInitialCondition;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isObservationModel;
import static crx.converter.engine.PharmMLTypeChecker.isParameter;
import static crx.converter.engine.PharmMLTypeChecker.isParameterEstimate;
import static crx.converter.engine.PharmMLTypeChecker.isPiece;
import static crx.converter.engine.PharmMLTypeChecker.isPiecewise;
import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import static crx.converter.engine.PharmMLTypeChecker.isRootType;
import static crx.converter.engine.PharmMLTypeChecker.isScalar;
import static crx.converter.engine.PharmMLTypeChecker.isSequence;
import static crx.converter.engine.PharmMLTypeChecker.isVector;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.Accessor;
import crx.converter.engine.BaseParser;
import crx.converter.engine.parts.Artifact;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.EstimationStep.ObjectiveFunctionParameter;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ObservationBlock.ObservationParameter;
import crx.converter.engine.parts.SimulationStep;
import crx.converter.engine.parts.StructuralBlock;
import crx.converter.engine.parts.TrialDesignBlock.ArmIndividual;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import eu.ddmore.converters.nonmem.statements.OmegaBlockStatement;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.Parameter;
import eu.ddmore.converters.nonmem.statements.PredStatement;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Constant;
import eu.ddmore.converters.nonmem.utils.Formatter.Param;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.IndependentVariableType;
import eu.ddmore.libpharmml.dom.commontypes.FunctionDefinitionType;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRefType;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.ConstantType;
import eu.ddmore.libpharmml.dom.maths.PieceType;
import eu.ddmore.libpharmml.dom.maths.PiecewiseType;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariateType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinitionType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel.GeneralCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.trialdesign.ActivityType;

public class Parser extends BaseParser {

	private static final String ENDLINE_CHAR = ";";
	ParametersHelper parameters;
	ArrayList<String> thetaSet = new ArrayList<String>();
	
	public Parser() throws IOException {
		comment_char = Formatter.COMMENT_CHAR;
		script_file_suffix = "ctl";
		objective_dataset_file_suffix = "csv";
		output_file_suffix = "csv";
		solver = "ode";
		setReferenceClass(getClass());
		init();
		
		Properties props = new Properties();
		props.load(getClass().getResourceAsStream("Parser.properties"));

		interpreterEXE = props.getProperty("interpreterEXE");
		if (props.containsKey("interpreterParams")) interpreterParams = props.getProperty("interpreterParams");
	}
	
	@Override
	protected String doSymbolRef(SymbolRefType o) {
		String symbol = unassigned_symbol;
		
		symbol = o.getSymbIdRef();

		Accessor a = lexer.getAccessor();
		PharmMLRootType element = a.fetchElement(o);
		
		if (isIndependentVariable(element)) 
			symbol = doIndependentVariable((IndependentVariableType) element);
		else
			symbol = Formatter.addPrefix(symbol);

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
		
		Properties binopProperties = new Properties();
		try {
			binopProperties.load(Parser.class.getResourceAsStream("binary_operator.properties"));
		} catch (IOException e) {
			//As overridden method, we are handling exception in common converter way. Need to do it better. 
			e.printStackTrace();
		}
		
		if(binopProperties.stringPropertyNames().contains(symbol)){
			return binopProperties.getProperty(symbol);
		}else{
			throw new IllegalStateException("Binary Operation not recognised : "+ symbol);
		}
	}
	
	@Override
	protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
		if (leaf == null) throw new NullPointerException("Tree leaf is NULL.");

		if (leaf.data != null) {
			boolean inPiecewise = false;
			if (isPiecewise(leaf.data)) inPiecewise = true;
			
			if (!isString_(leaf.data)) leaf.data = getSymbol(leaf.data);
			String current_value = "", current_symbol = "_FAKE_FAKE_FAKE_FAKE_";
			if(isLocalVariable(context)){
				String format = "%s = %s";
				current_symbol = Formatter.addPrefix(getSymbol(context));
				current_value = Formatter.endline(String.format(format, current_symbol, leaf.data));
			}else if (isDerivative(context) || isParameter(context)) {
				String format = "%s = %s", description = "";
				if (isRootType(context)) description = readDescription((PharmMLRootType) context);
				current_symbol = getSymbol(context);
				current_value = Formatter.endline(String.format(format, Formatter.addPrefix(description), leaf.data));
			} else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
				String format = "(%s) ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isPiece(context)) { 
				String format = "%s ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isContinuous(context)) {
				current_symbol = Formatter.addPrefix(getSymbol(context));
				String format = "%s = %s;";
				current_value = Formatter.endline(String.format(format, current_symbol, leaf.data));
			} else if (isActivity(context)) { 
				current_value = getSymbol(new ActivityDoseAmountBlock((ActivityType) context, (String) leaf.data));
			} else if (isIndividualParameter(context)) { 
				current_value = (String) leaf.data;
			} else if (isRandomVariable(context)) {
				ParameterRandomVariableType rv = (ParameterRandomVariableType) context;
				String format = "%s = %s;";
				current_value = Formatter.endline(String.format(format, rv.getSymbId(), (String) leaf.data));
			} else if (isCovariate(context)) { 
				CovariateDefinitionType cov = (CovariateDefinitionType) context;
				String format = "%s = %s;";
				current_value = Formatter.endline(String.format(format, cov.getSymbId(), (String) leaf.data));
			} else if (isObservationParameter(context)) {
				ObservationParameter op = (ObservationParameter) context;
				String format = "%s = %s;";
				current_value = Formatter.endline(String.format(format, op.getName(), (String) leaf.data));
			} else if (isCorrelation(context)) {
				current_value = (String) leaf.data;
			} else if (isObservationModel(context)) {
				current_value = Formatter.endline(((String) leaf.data).trim()) + Formatter.endline();
			} else if (isParameterEstimate(context) || context instanceof FixedParameter || context instanceof ObjectiveFunctionParameter) 
			{
				current_value = (String) leaf.data;
			} else {
				String format = " %s ";
				current_value = String.format(format, (String) leaf.data);
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
	protected String doIndependentVariable(IndependentVariableType v) {
		String symbol = v.getSymbId().toUpperCase();
		if (symbol.equals(Constant.T.toString()) || symbol.equals(ColumnConstant.TIME.toString())){
			symbol = (PredStatement.isDES)?Constant.T.toString():ColumnConstant.TIME.toString();
		}
		return symbol;
	}
	
	@Override
	protected String doConstant(ConstantType c) {
		String symbol = unassigned_symbol;
		
		String op = c.getOp();
		if (op.equalsIgnoreCase("notanumber")) symbol = "NaN";
		else if (op.equalsIgnoreCase("pi")) symbol = "3.1416";
		else if (op.equalsIgnoreCase("exponentiale")) symbol = "EXP(1)";
		else if (op.equalsIgnoreCase("infinity")) symbol = "INF";
	
		return symbol;
	}
	
	public String doPiecewise(PiecewiseType pw) {
		String symbol = unassigned_symbol;
		 
		List<PieceType> pieces = pw.getPiece();
		PieceType else_block = null;
		BinaryTree [] assignment_trees = new BinaryTree[pieces.size()]; 
		BinaryTree [] conditional_trees = new BinaryTree[pieces.size()];
		String [] conditional_stmts = new String [pieces.size()];
		String [] assignment_stmts = new String [pieces.size()];
			
		int assignment_count = 0, else_index = -1;
		for(int i = 0; i < pieces.size(); i++) {
			PieceType piece = pieces.get(i);
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
				BinaryTree assignment_tree = null;
				if (piece.getBinop() != null) { 
					assignment_tree = lexer.getTreeMaker().newInstance(getEquation(piece.getBinop()));
					assignment_count++;
				} else if (piece.getConstant() != null) {
					assignment_tree = lexer.getTreeMaker().newInstance(piece.getConstant());
					assignment_count++;
				} else if (piece.getFunctionCall() != null) {
					assignment_tree = lexer.getTreeMaker().newInstance(getEquation(piece.getFunctionCall()));
					assignment_count++;
				} else if (piece.getScalar() != null) {
					JAXBElement<?> scalar = piece.getScalar();
					if (isScalar(scalar)) {
						assignment_tree = lexer.getTreeMaker().newInstance(getEquation(piece.getScalar()));
						assignment_count++;
					}
				} else if (piece.getSymbRef() != null) {
					assignment_tree = lexer.getTreeMaker().newInstance(piece.getSymbRef());
					assignment_count++;
				}
				if (assignment_tree != null) assignment_trees[i] = assignment_tree;
			}
		}
			
		if (assignment_count == 0) throw new IllegalStateException("A piecewise block has no assignment statements.");
			
		
		for (int i = 0; i < pieces.size(); i++) {
			PieceType piece = pieces.get(i);
						
			if (conditional_trees[i] != null && assignment_trees[i] != null) {			
				// Logical condition
				if (!piece.equals(else_block)) conditional_stmts[i] = parse(piece, conditional_trees[i]);
		
				// Assignment block
				assignment_stmts[i] = parse(new Object(), assignment_trees[i]);
			}
		}
			
		int block_assignment = 0;
		StringBuilder block = new StringBuilder(Formatter.endline(" 0.0;"));
		for (int i = 0; i < pieces.size(); i++) {
			PieceType piece = pieces.get(i);
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
		if (assignment_count == 0) throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
		symbol = block.toString();
			
		return symbol;
	}
	
	Map<String, Integer> etasOrder = new HashMap<String, Integer>();

	@Override
	public void writeParameters(PrintWriter fout) {
		if (fout == null) return;
		if (lexer.getModelParameters().isEmpty()) {
			return;
		}
		parameters = new ParametersHelper(lexer.getScriptDefinition());
		setEtasOrder(parameters.createOrderedEtasMap());
		parameters.getParameters(lexer.getModelParameters());
		
		Map<String, ThetaStatement> thetas = parameters.getThetaParams();
		Map<String, OmegaStatement> omegas = parameters.getOmegaParams();
		OmegaBlockStatement omegaBlockStatement = parameters.getOmegaBlockStatement();
		Map<String, List<OmegaStatement>> omegaBlocks = omegaBlockStatement.getOmegaBlocks();
		
		List<String> sigmaParams = parameters.getSigmaStatements();
		
		setThetaAssigments();
		buildPredStatement(fout);

		if (!thetas.isEmpty()) {
			fout.write(Formatter.endline());
			fout.write(Formatter.endline("$"+Param.THETA));
			for (String thetaVar : thetas.keySet()) {
				writeParameter(thetas.get(thetaVar), fout);
			}
		}

		if(!omegaBlocks.isEmpty()){
			fout.write(Formatter.endline(omegaBlockStatement.getOmegaBlockTitle()));
			for(String eta : omegaBlockStatement.getOrderedEtasToOmegaMap().values()){
				for(OmegaStatement omega : omegaBlocks.get(eta)){					
					writeParameter(omega, fout);
				}
			}
		}
		if (!omegas.isEmpty()) {
			fout.write(Formatter.endline());
			fout.write(Formatter.endline("$"+Param.OMEGA));
			for (final String omegaVar : omegas.keySet()) {
				writeParameter(omegas.get(omegaVar), fout);
			}
		}
		if(!sigmaParams.isEmpty()){
			//adding default Omega if omega block is absent but sigma is present 
			if(omegas.isEmpty()){
				fout.write(Formatter.endline());
				fout.write(Formatter.endline("$"+Param.OMEGA+" 0 "+Constant.FIX));
			}
			fout.write(Formatter.endline());
			fout.write(Formatter.endline("$"+Param.SIGMA));
			for (final String sigmaVar: sigmaParams) {
				fout.write(sigmaVar);
			}
		}		
	}

	/**
	 * Write Theta and omega parameters according to the initial estimates, lower and upper bounds provided.
	 * 
	 * @param param
	 * @param simpleParam
	 * @param fout
	 */
	private void writeParameter(Parameter param, PrintWriter fout) {
		String description = param.getSymbId();
		
		ScalarRhs lowerBound = param.getLowerBound();
		ScalarRhs upperBound= param.getUpperBound(); 
		ScalarRhs initEstimate= param.getInitialEstimate();
		fout.write("(");
		writeParameterStatements(fout, description, lowerBound, upperBound,initEstimate);
		
		if(param.isFixed()){
			fout.write(" "+Constant.FIX+" ");
		}
		if(param.isStdDev()){
			fout.write(Constant.SD+" ");
		}
		fout.write(Formatter.endline(")"+Formatter.indent(Formatter.COMMENT_CHAR+description)));
	}

	/**
	 *  Writes parameter statement as described in following table,
	 *  
	 *  LB 	IN 	UB 	Action expected
	 *	X 	X 	X 	FAIL
	 *	X 	X 	Y 	FAIL
	 *	X 	Y 	X 	(IN)
	 *	Y 	X 	X 	FAIL
	 *	X 	Y 	Y 	(-INF,IN,UB)
	 *	Y 	Y 	X 	(LB,IN)
	 *	Y 	X 	Y 	(LB, ,UB)
	 *	Y 	Y 	Y 	(LB,IN,UB) 
	 * @param fout
	 * @param description
	 * @param lowerBound
	 * @param upperBound
	 * @param initEstimate
	 */
	private void writeParameterStatements(PrintWriter fout, String description,
			ScalarRhs lowerBound, ScalarRhs upperBound, ScalarRhs initEstimate) {
		
		if(lowerBound!=null){
			if(initEstimate!=null){
				if(upperBound!=null){
					writeStatement(lowerBound,initEstimate,upperBound,fout);
				}else{
					writeStatement(lowerBound,initEstimate,null,fout);
				}
			}else{
				if(upperBound!=null){
					writeStatement(lowerBound,null,upperBound,fout);
				}else{
					throw new IllegalStateException("Only lower bound value present for parameter : "+description);
				}
			}
		}else if(initEstimate!=null){
			if(upperBound!=null){
				fout.write("-INF,");
				writeStatement(null,initEstimate,upperBound,fout);
			}else{
				writeStatement(null,initEstimate,null,fout);
			}
		}else {
			throw new IllegalStateException("Only upper bound or no values present for parameter : "+description);
		}
	}
	
	/**
	 * Writes bound values of a parameter statement in expected format.
	 *  
	 * @param lowerBound
	 * @param init
	 * @param upperBound
	 * @param fout
	 */
	private void writeStatement(ScalarRhs lowerBound,ScalarRhs init,ScalarRhs upperBound, PrintWriter fout){
		if(lowerBound!=null){
			parse(lowerBound, lexer.getStatement(lowerBound), fout);
			fout.write(",");
		}
		if(init!=null){
			parse(init, lexer.getStatement(init), fout);
		}else{
			fout.write(", ,");
		}
		if(upperBound!=null){
			fout.write(",");
			parse(upperBound, lexer.getStatement(upperBound), fout);
		}
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
		thetaSet.addAll(parameters.getThetaParams().keySet());
	}
	
	/**
	 * 
	 * @param symbol
	 * @return
	 */
	public String getThetaForSymbol(String symbol){
		if(thetaSet.isEmpty()){
			setThetaAssigments();
		}
		if(thetaSet.contains(symbol)){
			symbol = String.format(Param.THETA+"(%s)",thetaSet.indexOf(symbol)+1);
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
	public String createIndividualDefinition(IndividualParameterType ip){
		
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
    			
    			List<CovariateRelationType> covariates = gaussianModel.getLinearCovariate().getCovariate();
    			if (covariates != null) {
    				for (CovariateRelationType covariate : covariates) {
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
    		statement.append(Formatter.endline(ENDLINE_CHAR));
    		
    		StringBuilder etas = addEtasStatementsToIndivParamDef(gaussianModel.getRandomEffects());
			if (logType.equals(Constant.LOG.toString())) {
				String format = Formatter.endline("%s = EXP(%s %s);");
				statement.append(String.format(format, Formatter.addPrefix(ip.getSymbId()), variableSymbol,etas));
			} else if (logType.equals(Constant.LOGIT.toString())) {
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
	private StringBuilder getCovariateForIndividualDefinition(CovariateRelationType covariate, PharmMLRootType type) {
		StringBuilder statement = new StringBuilder();
		if(type instanceof IndependentVariableType){
			String idvName = doIndependentVariable((IndependentVariableType)type);
			statement.append("+"+idvName);
		}
		else if(type instanceof CovariateDefinitionType){
			CovariateDefinitionType covariateDef = (CovariateDefinitionType) type;
			if (covariateDef != null) {
				if (covariateDef.getContinuous() != null) {
					String covStatement = "";
					ContinuousCovariateType continuous = covariateDef.getContinuous();
					if (continuous.getTransformation() != null) covStatement = getSymbol(continuous.getTransformation());
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
	private String addFixedEffectsStatementToIndivParamDef(CovariateRelationType covariate, String covStatement) {
		List<FixedEffectRelationType> fixedEffects = covariate.getFixedEffect();
		if (fixedEffects != null) {
			for (FixedEffectRelationType fixed_effect : fixedEffects) {
				if (fixed_effect == null) continue;
				String  fixedEffectStatement = Formatter.addPrefix(fixed_effect.getSymbRef().getSymbIdRef());
				if(fixedEffectStatement.isEmpty())
					fixedEffectStatement = parse(fixed_effect);
				covStatement = fixedEffectStatement + " * " + covStatement;
				break;
			}
		}
		return covStatement;
	}
	
	/**
	 * This method will return individual definition details if assignment is present.
	 * @param ip
	 * @return
	 */
	private StringBuilder getIndivDefinitionForAssign(IndividualParameterType ip) {
		StringBuilder statement = new StringBuilder();
		if (ip.getAssign() != null) {
			statement.append(Formatter.endline());
			statement.append(String.format("%s = ", ip.getSymbId()));
			String assignment = parse(new Object(), lexer.getStatement(ip.getAssign()));
			statement.append(Formatter.endline(assignment+ENDLINE_CHAR));
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
	private String getLogType(LhsTransformationType transform) {
		if (transform == LhsTransformationType.LOG){
			return Constant.LOG.toString();
		}else if (transform == LhsTransformationType.LOGIT){
			return Constant.LOGIT.toString();
		}else{
			throw new  UnsupportedOperationException("Tranformation type "+transform.name()+" not yet supported");
		}
	}
	
	/**
	 * This method adds etas from random effects to individual parameter definitions.
	 * @param random_effects
	 * @return
	 */
	private StringBuilder addEtasStatementsToIndivParamDef(List<ParameterRandomEffectType> random_effects) {
		StringBuilder etas = new StringBuilder();
		if (random_effects != null && !random_effects.isEmpty()) {
			for (ParameterRandomEffectType random_effect : random_effects) {
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
	
	@Override
	public void writeInterpreterPath(PrintWriter fout) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeFunctions(PrintWriter fout,
			List<FunctionDefinitionType> functions) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeModelFunction(PrintWriter fout, StructuralBlock sb)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeFunction(FunctionDefinitionType func, String output_dir)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeEstimationPKPD(PrintWriter fout, EstimationStep block,
			String output_dir) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeScriptLibraryReferences(PrintWriter fout)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeTimeSpan(PrintWriter fout, SimulationStep step)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeInitialConditions(PrintWriter fout, StructuralBlock block)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeSimulation(PrintWriter fout, StructuralBlock sb)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeSimulationOptions(PrintWriter fout) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Artifact> writeContinuous(PrintWriter fout, SimulationStep step) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String writeSaveCommand(PrintWriter fout, List<Artifact> refs,
			String output_dir, String id1, String id2) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeArmLoop(PrintWriter fout, List<ArmIndividual> list)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeInitObservationBlockParameters(PrintWriter fout,
			List<ObservationBlock> obs) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeVariableAssignments(PrintWriter fout, SimulationStep step)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeErrorModel(PrintWriter fout, List<ObservationBlock> ems)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writePlottingBlockPkPdSimulation(PrintWriter fout,
			List<Artifact> refs) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writePlottingBlockPkPdSimulationWithDosing(PrintWriter fout)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeExternallyReferencedElement(PrintWriter fout,
			StructuralBlock sb) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
