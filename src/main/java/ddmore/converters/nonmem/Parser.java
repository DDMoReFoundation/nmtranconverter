
package ddmore.converters.nonmem;

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
import ddmore.converters.nonmem.statements.OmegaStatement;
import ddmore.converters.nonmem.statements.Parameter;
import ddmore.converters.nonmem.statements.PredStatement;
import ddmore.converters.nonmem.statements.SigmaStatement;
import ddmore.converters.nonmem.statements.ThetaStatement;
import ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.IndependentVariableType;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.FunctionDefinitionType;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRefType;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinitionType;
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
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel.LinearCovariate.PopulationParameter;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameterType;
import eu.ddmore.libpharmml.dom.trialdesign.ActivityType;

public class Parser extends BaseParser {
	ParametersHelper parameters;
	ArrayList<String> thetaSet = new ArrayList<String>();
	private final String THETA = "THETA";
	
	public ParametersHelper getParameters() {
		return parameters;
	}

	public Parser() throws IOException {
		comment_char = "$";
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
		
		if (isIndependentVariable(element)) symbol = doIndependentVariable((IndependentVariableType) element);
		else if (lexer.isModelParameter(symbol)) {
			Integer idx = lexer.getModelParameterIndex(symbol);
			String format = "%s_%s";
			symbol = String.format(format, THETA, idx);
		}
		//TODO: need to verify NM_ appending to parameters.
//		else if (lexer.isStateVariable(symbol)) {
////			Integer idx = lexer.getStateVariableIndex(symbol);
//			String format = "%s_%s";
//			symbol = String.format(format, "NM", symbol.toUpperCase());
//		} else if (lexer.isIndividualParameter(symbol)) {
//			Integer idx = lexer.getIndividualParameterIndex(symbol);
//			String format = "%s_%s";
//			symbol = String.format(format, "NM", symbol.toUpperCase());
//		}
		
		return "NM_"+symbol;
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
				String format = "%s = %s\n";
				current_symbol = "NM_"+getSymbol(context);
				current_value = String.format(format, current_symbol, leaf.data);
			}else if (isDerivative(context) || isParameter(context)) {
				String format = "%s = %s\n", description = "";
				if (isRootType(context)) description = readDescription((PharmMLRootType) context);
				current_symbol = getSymbol(context);
				current_value = String.format(format, "NM_"+description, leaf.data);
			} else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
				String format = "(%s) ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isPiece(context)) { 
				String format = "%s ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isContinuous(context)) {
				current_symbol = "NM_"+getSymbol(context);
				String format = "%s = %s; \n";
				current_value = String.format(format, current_symbol, leaf.data);
			} else if (isActivity(context)) { 
				current_value = getSymbol(new ActivityDoseAmountBlock((ActivityType) context, (String) leaf.data));
			} else if (isIndividualParameter(context)) { 
				current_value = (String) leaf.data;
			} else if (isRandomVariable(context)) {
				ParameterRandomVariableType rv = (ParameterRandomVariableType) context;
				String format = "%s = %s;\n";
				current_value = String.format(format, rv.getSymbId(), (String) leaf.data);
			} else if (isCovariate(context)) { 
				CovariateDefinitionType cov = (CovariateDefinitionType) context;
				String format = "%s = %s;\n";
				current_value = String.format(format, cov.getSymbId(), (String) leaf.data);
			} else if (isObservationParameter(context)) {
				ObservationParameter op = (ObservationParameter) context;
				String format = "%s = %s;\n";
				current_value = String.format(format, op.getName(), (String) leaf.data);
			} else if (isCorrelation(context)) {
				current_value = (String) leaf.data;
			} else if (isObservationModel(context)) {
				current_value = ((String) leaf.data).trim() + "\n\n";
			} else if (isParameterEstimate(context) || context instanceof FixedParameter || context instanceof ObjectiveFunctionParameter) 
			{
				current_value = (String) leaf.data;
			} else {
				String format = " %s ";
				current_value = String.format(format, (String) leaf.data);
			} 
			
			if (current_value != null) {
				if(inPiecewise) {
					if (current_symbol != null) current_value = current_value.replaceAll(field_tag, current_symbol) + "\n";
				}
				fout.write(current_value);
			}
		} else
			throw new IllegalStateException("Should be a statement string bound to the root.data element.");
	}
	
	@Override
	protected String doIndependentVariable(IndependentVariableType v) {
		String symbol = v.getSymbId();
		if (symbol.equals("t") || symbol.equals("time")) symbol = "TIME";
		return symbol;
	}
	
	@Override
	protected String doConstant(ConstantType c) {
		String symbol = unassigned_symbol;
		
		String op = c.getOp();
		if (op.equalsIgnoreCase("notanumber")) symbol = "NaN";
		else if (op.equalsIgnoreCase("pi")) symbol = "pi";
		else if (op.equalsIgnoreCase("exponentiale")) symbol = "exp(1)";
		else if (op.equalsIgnoreCase("infinity")) symbol = "Inf";
	
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
		StringBuilder block = new StringBuilder(" NaN;\n");
		for (int i = 0; i < pieces.size(); i++) {
			PieceType piece = pieces.get(i);
			if (piece == null) continue;
			else if (piece.equals(else_block)) continue;
			
			if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) continue;	
				String operator = "IF", format = "%s (%s) {\n %s = %s \n";
				if (block_assignment > 0) {
				operator = "} ELSE IF ";
				format = " %s (%s) {\n %s = %s \n";
			}
				
			block.append(String.format(format, operator, conditional_stmts[i], field_tag, assignment_stmts[i]));
			block_assignment++;
		}
		
		if (else_block != null && else_index >= 0) {
			block.append("} ELSE {\n");
			String format = " %s = %s\n";
			block.append(String.format(format, field_tag, assignment_stmts[else_index]));
		}
		block.append("}");
		if (assignment_count == 0) throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
		symbol = block.toString();
			
		return symbol;
	}

	@Override
	public void writeParameters(PrintWriter fout) {
		
		if (fout == null) return;
		
		if (lexer.getModelParameters().isEmpty()) {
			return;
		}
		
		parameters = new ParametersHelper(lexer.getScriptDefinition());
		
		parameters.getParameters(lexer.getModelParameters());
		
		Map<String, SimpleParameterType> simpleParameters= parameters.getSimpleParams();
		Map<String, ThetaStatement> thetas = parameters.getThetaParams();
		Map<String, OmegaStatement> omegas = parameters.getOmegaParams();

		SigmaStatement sigmaStatement = new SigmaStatement(parameters);
		List<String> sigmaParams = sigmaStatement.getSigmaStatement();
		
		setThetaAssigments();
		buildPredStatement(fout);

		if (! thetas.isEmpty()) {
			fout.write("\n$"+THETA+"\n");
			for (String thetaVar : thetas.keySet()) {
				writeParameter(thetas.get(thetaVar), simpleParameters.get(thetaVar), fout);
			}
		}

		if (! omegas.isEmpty()) {
			fout.write("\n$OMEGA\n");
			for (final String omegaVar : omegas.keySet()) {
				writeParameter(omegas.get(omegaVar), simpleParameters.get(omegaVar), fout);
			}
		}

		if(!sigmaParams.isEmpty()){
			fout.write("\n$SIGMA\n");
			for (final String sigmaVar: sigmaParams) {
				fout.write(sigmaVar);
			}
		}		
	}

	/**
	 * Write Theta and omega parameters according to the initial estimates, lower and upper bounds provided.
	 * TODO: Fixed parameter handling needs to be confirmed with @ Henrik.
	 * 
	 * @param param
	 * @param simpleParam
	 * @param fout
	 */
	private void writeParameter(Parameter param, SimpleParameterType simpleParam, PrintWriter fout) {
		String startParens = "(";
		String endParens = ") ; ";
		String description = readDescription((PharmMLRootType) simpleParam);
		if(description.isEmpty()){
			description = param.getSymbId();
		}
		
		fout.write(startParens);
		if (param.getLowerBound() != null && param.getUpperBound() != null) {
			
			//TODO: What to do if param.isFixed is true ?
			parse(param.getLowerBound(), lexer.getStatement(param.getLowerBound()), fout);
			if (param.getInitialEstimate() != null) {
				fout.write(",");
				parse(param.getInitialEstimate(), lexer.getStatement(param.getInitialEstimate()), fout);
			}
			fout.write(",");
			parse(param.getUpperBound(), lexer.getStatement(param.getUpperBound()), fout);			
		} else if (param.getInitialEstimate() != null) {
			parse(param.getInitialEstimate(), lexer.getStatement(param.getInitialEstimate()), fout);
			//Add 'FIX' if its fixed parameter
			endParens = (param.isFixed())? ", FIX"+endParens : endParens; 
		}else {
			// Just use the initial value defined in the ParameterModel block
			//TODO: this approach needs to be confirmed
			parse(simpleParam, lexer.getStatement(simpleParam), fout);
		}
		fout.write(endParens + description + "\n");
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
			symbol = String.format(THETA+"(%s)",thetaSet.indexOf(symbol)+1);
		}
		return symbol;
	}

	int nRandomEffects = 0;
	
	/**
	 * Get individual parameter assignments from individual parameter type.
	 * This method is used as part of pred core block.
	 * 
	 * @param ip
	 * @return
	 */
	public String doIndividualParameterAssignment(IndividualParameterType ip) {

    	StringBuilder stmt = new StringBuilder();
    	
    	String variableSymbol = ip.getSymbId();    	
    	if (ip.getAssign() != null) {
    		stmt.append(String.format("\n%s = ", variableSymbol));
    		String assignment = parse(new Object(), lexer.getStatement(ip.getAssign()));
    		stmt.append(assignment);
    		stmt.append(";\n");
    	} else if (ip.getGaussianModel() != null) {
    		GaussianModel m = ip.getGaussianModel();
    		LhsTransformationType transform = m.getTransformation();
    		GaussianModel.GeneralCovariate gcov = m.getGeneralCovariate();
    		GaussianModel.LinearCovariate lcov = m.getLinearCovariate();
    		List<ParameterRandomEffectType> random_effects = m.getRandomEffects();
    		
    		int nCovs = 0;
    		if (transform == LhsTransformationType.LOG) variableSymbol = "log" + capitalise(variableSymbol);
    		else if (transform == LhsTransformationType.LOGIT) variableSymbol = "logit" + capitalise(variableSymbol);
    		
    		stmt.append(String.format("%s = ", variableSymbol));
    		if (lcov != null) {
    			String pop_param_symbol = null;
    			
    			PopulationParameter pop_param = lcov.getPopulationParameter();
    			if (pop_param != null) {
    				pop_param_symbol = getThetaForSymbol(pop_param.getAssign().getSymbRef().getSymbIdRef());
    				if (transform == LhsTransformationType.LOG) pop_param_symbol = String.format("LOG(%s)", pop_param_symbol);
    	    		else if (transform == LhsTransformationType.LOGIT) pop_param_symbol = String.format("LOGIT(%s)", pop_param_symbol);
    				stmt.append(String.format("(%s)", pop_param_symbol));
    			}
    			
    			List<CovariateRelationType> covariates = lcov.getCovariate();
    			if (covariates != null) {
    				if (pop_param_symbol != null) stmt.append(" + ");
    				
    				for (CovariateRelationType covariate : covariates) {
    					if (covariate == null) continue;
    					if (nCovs > 0) stmt.append(" + ");
    					
    					CovariateDefinitionType cdt = (CovariateDefinitionType) lexer.getAccessor().fetchElement(covariate.getSymbRef());
    					if (cdt != null) {
    						if (cdt.getContinuous() != null) {
    							String cov_stmt = "";
    							ContinuousCovariateType continuous = cdt.getContinuous();
    							if (continuous.getTransformation() != null) cov_stmt = getSymbol(continuous.getTransformation());
    							else cov_stmt = cdt.getSymbId();
    							
    							List<FixedEffectRelationType> fixed_effects = covariate.getFixedEffect();
    							if (fixed_effects != null) {
    								for (FixedEffectRelationType fixed_effect : fixed_effects) {
    									if (fixed_effect == null) continue;
    									String  fixed_effect_stmt = getThetaForSymbol(fixed_effect.getSymbRef().getSymbIdRef());
    									if(fixed_effect_stmt.isEmpty())
    										fixed_effect_stmt = parse(fixed_effect, lexer.getStatement(fixed_effect));
    									cov_stmt = fixed_effect_stmt + " * " + cov_stmt;
    									break;
    								}
    							}
    							stmt.append(cov_stmt);
    							nCovs++;
    						} else if (cdt.getCategorical() != null) {
    							throw new UnsupportedOperationException("No categorical yet");
    						}
    					}
    				}
    			}
    		} else if (gcov != null) {
    			String assignment = parse(gcov, lexer.getStatement(gcov));
    			stmt.append(assignment);
    			nCovs++;
    		}
    		
			if (random_effects != null) {
				if (!random_effects.isEmpty()) {
					if (nCovs > 0) stmt.append(" + ");
					for (ParameterRandomEffectType random_effect : random_effects) {
						if (random_effect == null) continue;
						++nRandomEffects;
						stmt.append(" ETA("+ nRandomEffects+")");
					}
				}
			}
			stmt.append(";\n");
			
			if (transform == LhsTransformationType.LOG) {
				String format = "%s = EXP(%s);\n";
				stmt.append(String.format(format, "NM_"+ip.getSymbId(), variableSymbol));
			} else if (transform == LhsTransformationType.LOGIT) {
				String format = "%s = 1./(1 + exp(-%s));\n";
				stmt.append(String.format(format, "NM_"+ip.getSymbId(), variableSymbol));
			}
    	}
		stmt.append("\n");
		
		return stmt.toString();
	}

	@Override
	public String getScriptFilename(String output_dir) {
		String format = "%s/%s.%s";
		return String.format(format, output_dir, run_id, script_file_suffix);
	}
	
	/**
	 * Method to be used by other classes which doesnt have visibility to BaseParser
	 * @param context
	 * @return
	 */
	public String parse(Object context){
		return parse(context, lexer.getStatement(context));
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
