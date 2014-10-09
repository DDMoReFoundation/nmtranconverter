
package ddmore.converters.nonmem;

import static crx.converter.engine.PharmMLTypeChecker.isActivity;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isGaussianError;
import static crx.converter.engine.PharmMLTypeChecker.isGeneralError;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.Accessor;
import crx.converter.engine.BaseParser;
import crx.converter.engine.parts.Artifact;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import crx.converter.engine.parts.EstimationStep.ObjectiveFunctionParameter;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ObservationBlock.ObservationParameter;
import crx.converter.engine.parts.SimulationStep;
import crx.converter.engine.parts.StructuralBlock;
import crx.converter.engine.parts.TrialDesignBlock.ArmIndividual;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import crx.converter.tree.TreeMaker;
import ddmore.converters.nonmem.statements.SigmaStatement;
import ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.IndependentVariableType;
import eu.ddmore.libpharmml.dom.commontypes.CommonVariableDefinitionType;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.FunctionDefinitionType;
import eu.ddmore.libpharmml.dom.commontypes.InitialConditionType;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.commontypes.SequenceType;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRefType;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinitionType;
import eu.ddmore.libpharmml.dom.commontypes.VectorType;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.ConstantType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;
import eu.ddmore.libpharmml.dom.maths.PieceType;
import eu.ddmore.libpharmml.dom.maths.PiecewiseType;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariateType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinitionType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelationType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffectType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameterType;
import eu.ddmore.libpharmml.dom.modeldefn.StructuralModelType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType.GaussianModel.LinearCovariate.PopulationParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ContinuousObservationType;
import eu.ddmore.libpharmml.dom.modellingsteps.InitialEstimateType;
import eu.ddmore.libpharmml.dom.modellingsteps.ObservationsType;
import eu.ddmore.libpharmml.dom.modellingsteps.TimepointsType;
import eu.ddmore.libpharmml.dom.trialdesign.ActivityType;

public class Parser extends BaseParser {
	private HashMap<List<Artifact>, String> outputs_to_csvfile_map = new HashMap<List<Artifact>, String>();
	private String plot_data_symbol = "plot_data";
	
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
			String format = "%s[%s]";
			symbol = String.format(format, param_model_symbol, idx);
		} else if (lexer.isStateVariable(symbol)) {
			Integer idx = lexer.getStateVariableIndex(symbol);
			String format = "%s[%s]";
			symbol = String.format(format, state_vector_symbol, idx);
		} else if (lexer.isIndividualParameter(symbol)) {
			Integer idx = lexer.getIndividualParameterIndex(symbol);
			String format = "%s[%s]";
			symbol = String.format(format, indiv_param_symbol, idx);
		}
		
		return symbol;
	}
	
	@Override
	protected String doDerivative(DerivativeVariableType o) {
		String symbol = unassigned_symbol;
		
		Integer idx = lexer.getStateVariableIndex(o.getSymbId());
		String format = "sprime[%s]";
		symbol = String.format(format, idx);
		
		return symbol;
	}
	
	@Override
	protected String doParameter(SimpleParameterType p) {
		if (lexer.isModelParameter(p.getSymbId())) {
//			Integer idx = lexer.getModelParameterIndex(p.getSymbId());
//			String format = "%s[%s]";
//			return String.format(format, param_model_symbol, idx);
			return p.getSymbId();
		} else 
			return p.getSymbId();
	}
	
	@Override
	protected String doIndependentVariable(IndependentVariableType v) {
		String symbol = v.getSymbId();
		
		if (symbol.equals("t") || symbol.equals("time")) symbol = "TIME";
		
		return symbol;
	}
	
	@Override
	protected String doFalse() {
		return "FALSE";
	}
	
	@Override
	protected String doTrue() {
		return "TRUE";
	}
	
	@Override
	protected String doSequence(SequenceType o) {
		String symbol = unassigned_symbol;
		
		SequenceType seq = (SequenceType) o;
		Rhs begin = seq.getBegin();
		Rhs end = seq.getEnd();
		Rhs repetitions = seq.getRepetitions();
		Rhs stepSize = seq.getStepSize();
		
		BinaryTree btBegin = null, btEnd = null, btRepetitions = null, btStepSize = null;
		if (begin == null) throw new IllegalStateException("The required Sequence.begin field is not assigned.");
		
		if (begin != null) btBegin = lexer.getStatement(begin);
		if (end != null) btEnd = lexer.getStatement(end);
		if (repetitions != null) btRepetitions = lexer.getStatement(repetitions);
		if (stepSize != null) btStepSize = lexer.getStatement(stepSize);
		
		String strBegin = null, strEnd = null, strRepetitions = null, strStepSize = null;
		
		if (btBegin != null) strBegin = parse(seq, btBegin);
		if (btEnd != null) strEnd = parse(seq, btEnd);	
		if (btRepetitions != null) strRepetitions = parse(seq, btRepetitions);
		if (btStepSize != null)	strStepSize = parse(seq, btStepSize);
			
		// Default value in case the conditional logic fails or the PharmML spec changes.
		symbol = "c(0 0)";
		if (strBegin != null && strEnd != null && strStepSize != null) {
			String format = "seq(%s, %s, by=%s)";
			symbol = String.format(format, strBegin, strEnd, strStepSize);
		} else if (strBegin != null && strEnd != null && strRepetitions != null) {
			String format = "seq(%s,%s,length=%s)";
			symbol = String.format(format, strBegin, strEnd, strRepetitions);
		} else if (strBegin != null && strStepSize != null && strRepetitions != null) {
			String format = "seq(%s, (%s*%s), by=%s)";
			symbol = String.format(format, strBegin, strStepSize, strRepetitions, strStepSize);
		} else if (strBegin != null && strEnd != null) {
			String format = "%s:%s";
			symbol = String.format(format, strBegin, strEnd);
		}
		
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
	
	@Override
	protected String doVector(VectorType v) {
		String symbol = unassigned_symbol;
		
		ArrayList<String> elements = new ArrayList<String>();
		
		for (JAXBElement<?> element : v.getSequenceOrScalar()) {
			if (element == null) continue;
			Object value = element.getValue();
			if (value != null) {
				String parsed_value = parse(v, lexer.getStatement(value));
				elements.add(parsed_value);
			}
		}
		
		StringBuilder st = new StringBuilder();
		st.append("c(");
		int count = 0;
		for (String element : elements) {
			if (count > 0) st.append(",");
			st.append(element.trim());
			count++;
		}
		st.append(")");
		
		symbol = st.toString();
		
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
				String operator = "if", format = "%s (%s) {\n %s <- %s \n";
				if (block_assignment > 0) {
				operator = "} else if ";
				format = " %s (%s) {\n %s <- %s \n";
			}
				
			block.append(String.format(format, operator, conditional_stmts[i], field_tag, assignment_stmts[i]));
			block_assignment++;
		}
		
		if (else_block != null && else_index >= 0) {
			block.append("} else {\n");
			String format = " %s <- %s\n";
			block.append(String.format(format, field_tag, assignment_stmts[else_index]));
		}
		block.append("}");
		if (assignment_count == 0) throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
		symbol = block.toString();
			
		return symbol;
	}
	
	@Override
	protected String doFunctionCall(FunctionCallType call) {
		if (call == null) throw new NullPointerException("A function call definition is null.");

		ArrayList<String> arg_symbols = new ArrayList<String>();
		for (FunctionArgument arg : call.getFunctionArgument()) {
			String arg_symbol = "0.0";
			if (arg.getConstant() != null)
				arg_symbol = getSymbol(arg.getConstant());
			else if (arg.getScalar() != null) {
				Object v = arg.getScalar().getValue();
				arg_symbol = getSymbol(v);
			} else if (arg.getSymbRef() != null)
				arg_symbol = getSymbol(arg.getSymbRef());
			else if (arg.getEquation() != null) {
				arg_symbol = getSymbol(arg.getEquation());
			}
			arg_symbols.add(arg_symbol);
		}

		StringBuffer args_string = new StringBuffer();
		int i = 0;
		for (String symbol : arg_symbols) {
			if (i > 0)
				args_string.append(",");
			args_string.append(symbol);
			i++;
		}

		String format = "(%s(%s))";
		
		return String.format(format, call.getSymbRef().getSymbIdRef(), args_string);
	}

	@Override
	public void writeFunction(FunctionDefinitionType func, String output_dir) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void writeInterpreterPath(PrintWriter fout) {
		if (fout == null) return;
		if (interpreterEXE != null) {
			if (interpreterEXE.length() > 0) {
				String format = "#!%s %s\n\n";
				fout.write(String.format(format, interpreterEXE, interpreterParams));
			}
		}
	}
	
	@Override
	public void writeParameters(PrintWriter fout) {
		
		if (fout == null) return;
		
		if (lexer.getModelParameters().isEmpty()) {
			return;
		}
		
		ParametersHelper parameters = new ParametersHelper(lexer.getScriptDefinition());
		
		parameters.getParameters(lexer.getModelParameters());
		
		Map<String, SimpleParameterType> simpleParameters= parameters.getSimpleParams();
		Map<String, InitialEstimateType> initialEstimates = parameters.getInitialEstimates();
		Map<String, ScalarRhs> lowerBoundsMap = parameters.getLowerBounds();
		Map<String, ScalarRhs> upperBoundsMap = parameters.getUpperBounds();
		List<String> thetas = parameters.getThetaParams();
		List<String> omegas = parameters.getOmegaParams();
		
		SigmaStatement sigmaStatement = new SigmaStatement(parameters);
		List<String> sigmaParams = sigmaStatement.getSigmaStatement();
		
		if (! thetas.isEmpty()) {
			fout.write("\n$THETA\n");
			for (final String thetaVar : thetas) {
				writeParameter(thetaVar, initialEstimates.get(thetaVar), lowerBoundsMap.get(thetaVar), upperBoundsMap.get(thetaVar), simpleParameters.get(thetaVar), fout);
			}
		}
		
		if (! omegas.isEmpty()) {
			fout.write("\n$OMEGA\n");
			for (final String omegaVar : omegas) {
				writeParameter(omegaVar, initialEstimates.get(omegaVar), lowerBoundsMap.get(omegaVar), upperBoundsMap.get(omegaVar), simpleParameters.get(omegaVar), fout);
			}
		}
		
		if(!sigmaParams.isEmpty()){
			fout.write("\n$SIGMA\n");
			for (final String sigmaVar: sigmaParams) {
				fout.write(sigmaVar);
			}
		}

		fout.write("\n");
		List<ParameterBlock> blocks = lexer.getScriptDefinition().getParameterBlocks();
		for(ParameterBlock parameterBlock : blocks){
			for(IndividualParameterType parameterType: parameterBlock.getIndividualParameters()){
				fout.write(doIndividualParameterAssignment(parameterType));	
			}
			
		}
		
	}
	
	
	private void writeParameter(final String symbolId,
								final InitialEstimateType initialEst, final ScalarRhs lowerBound, final ScalarRhs upperBound, final SimpleParameterType simpleParam,
								final PrintWriter fout) {
		
		String description = readDescription((PharmMLRootType) simpleParam);
		if(description.isEmpty()){
			description = symbolId;
		}
		
		if (lowerBound != null && upperBound != null) {
			fout.write("(");
			parse(lowerBound, lexer.getStatement(lowerBound), fout);
			if (initialEst != null) {
				fout.write(",");
				parse(initialEst, lexer.getStatement(initialEst), fout);
			}
			fout.write(",");
			parse(upperBound, lexer.getStatement(upperBound), fout);
			fout.write(") ; " + description + "\n");
		} else if (initialEst != null) {
			fout.write("(");
			parse(initialEst, lexer.getStatement(initialEst), fout);
			fout.write(") ; " + description + "\n");
		} else {
			// Just use the initial value defined in the ParameterModel block
			parse(simpleParam, lexer.getStatement(simpleParam), fout);
		}
	}
	
	@Override
	public void writeScriptLibraryReferences(PrintWriter fout) throws IOException {
		if (fout == null) return;
		
		fout.write("library(\"pracma\")\n");
		fout.write("library(\"deSolve\")\n\n");
	}
	
	private void writeSourceGeneratedFuncs(PrintWriter fout) {
		if (fout == null) return;
		
		if (!generated_funcs.isEmpty()) {
			String format = "source(\"%s\")\n";
			for (String generated_func : generated_funcs) {
				if (generated_func == null) continue;
				generated_func = generated_func.replace("\\", "/");
				fout.write(String.format(format, generated_func));
			}
		}
	}
	
	@Override
	public void writeFunctions(PrintWriter fout, List<FunctionDefinitionType> functions) throws IOException {
	}
	
	private String getIndependentVariableSymbol() {
		return getSymbol(lexer.getDom().getIndependentVariable());
	}
	
	@Override
	public void writeScriptHeader(PrintWriter fout, String model_file)
			throws IOException {
		
	}

	@Override
	public void writeModelFunction(PrintWriter fout, StructuralBlock sb) throws IOException {
		if (fout == null) throw new NullPointerException();
		
		String format = null;
		if (lexer.isMixedEffect()) {
			format = "%s <- function (%s, %s, %s, %s) {\n";
			fout.write(String.format(format, getModelFunctionName(sb), getIndependentVariableSymbol(), state_vector_symbol, param_model_symbol, indiv_param_symbol));
			
			format = "with(as.list(c(%s, %s, %s, %s)), {\n";
			fout.write(String.format(format, getIndependentVariableSymbol(), state_vector_symbol, param_model_symbol, indiv_param_symbol));
		} else {
//			format = "%s <- function (%s, %s, %s) {\n";
//			fout.write(String.format(format, getModelFunctionName(sb), getIndependentVariableSymbol(), state_vector_symbol, param_model_symbol));
//			
//			format = "with(as.list(c(%s, %s, %s)), {\n";
//			fout.write(String.format(format, getIndependentVariableSymbol(), state_vector_symbol, param_model_symbol, indiv_param_symbol));
		}
		
		SimulationStep step = lexer.getSimulationStep();
		
		StructuralModelType smt = sb.getModel();
		if (smt ==  null) throw new NullPointerException("Structural model is NULL.");

//		format = "sprime <- rep.int(0.0, length(%s));\n"; TODO
//		fout.write(String.format(format, state_vector_symbol));
		
		// Continuous assignment from the sprime input buffer.
		if (step != null && sb != null) {
			if (step.hasContinuous()) {
				for (JAXBElement<? extends CommonVariableDefinitionType> var : smt.getCommonVariable()) {
					CommonVariableDefinitionType value = var.getValue();
					if (value instanceof VariableDefinitionType) {
						VariableDefinitionType local = (VariableDefinitionType) value;
						if (step.isPerStepVariable(local)) {
							int idx = step.getPerStepVariableIndex(local);
//							format = "\n%s <- %s[%d];\n"; TODO
//							fout.write(String.format(format, local.getSymbId(), state_vector_symbol, idx));
						}
					}
				}
				fout.write("\n");
			}
		}
		
		// Assuming that the PharmML contains a single structural model definition.
		for (JAXBElement<? extends CommonVariableDefinitionType> var : smt.getCommonVariable()) {
			CommonVariableDefinitionType value = var.getValue();
			if (value == null) throw new NullPointerException("Structural model component is NULL");
					
			if (value instanceof VariableDefinitionType || value instanceof DerivativeVariableType) {
				if (!lexer.hasStatement(value)) continue; 
				parse(value, lexer.getStatement(value), fout);
				fout.write("\n");
			}	
		}
		
		// Check for continuous variable assignment in the model function.
		if (step != null && sb != null) {
			if (step.hasContinuous()) {
				for (JAXBElement<? extends CommonVariableDefinitionType> var : smt.getCommonVariable()) {
					CommonVariableDefinitionType value = var.getValue();
					if (value instanceof VariableDefinitionType) {
						VariableDefinitionType local = (VariableDefinitionType) value;
						if (step.isPerStepVariable(local)) {
							int idx = -1;
							if (step != null) idx = step.getPerStepVariableIndex(local);
							format = "sprime[%d] <- %s;\n";
							fout.write(String.format(format, idx, local.getSymbId()));
						}
					}
				}
			}
			fout.write("\n");
		}
		
		// Return type of the function.
		fout.write("\nreturn(list(c(sprime)))\n})}\n\n");
	}
	
	@Override
	public void writePlottingBlockPkPdSimulation(PrintWriter fout, List<Artifact> refs) throws IOException {
		
		if (fout == null) throw new NullPointerException("Output script stream is NULL.");
		if (refs.isEmpty()) return;
		
		String filepath = outputs_to_csvfile_map.get(refs);
		if (filepath == null) return;
		
		writePurgeCommand(fout);
		writePlotWarning(fout);
		writeLoadCSVCommand(fout, plot_data_symbol, filepath);
		
		String format = "matplot(%s[,1],%s[,-1],type=\"l\");\n";
		fout.write(String.format(format, plot_data_symbol, plot_data_symbol));
		
		format = "cols <- names(%s);\n";
		fout.write(String.format(format, plot_data_symbol));
		
		format = "legend(\"topright\", legend=c(cols[-1]), pch=21, col=c(2,4), horiz=FALSE, cex=0.50);\n";
		fout.write(String.format(format, plot_data_symbol));
	}
	
	private void writeLoadCSVCommand(PrintWriter fout, String df_name, String filepath) {
		if (fout == null || df_name == null || filepath == null) return;
		
		String format = "%s <- read.csv(\"%s\");\n";
		fout.write(String.format(format, df_name, filepath));
	}

	@Override
	public void writeEstimationPKPD(PrintWriter fout, EstimationStep block, String output_dir) throws IOException {
	}

	@Override
	public void writeTimeSpan(PrintWriter fout, SimulationStep step) throws IOException {
if (fout == null || step == null) return;
		
		String format = "%s <- c(0, 0);\n";
		HashMap<ObservationsType, TimepointsType> tspan_map = step.getTimePoints();
		if (tspan_map.values().size() == 0) {
			fout.write(String.format(format, tspan_symbol));
			return;
		}
		
		ArrayList<String> times = new ArrayList<String>(); 
		for (TimepointsType time : tspan_map.values()) {
			if (time == null) continue;
			BinaryTree bt = lexer.getStatement(time);
			if (bt == null) throw new NullPointerException("Time range has not binary tree (not parsed).");
			times.add(parse(time, bt));
		}
		
		// Write out the local time variables.
		format = "\n%s Observation Time Range(s)\n";
		fout.write(String.format(format, comment_char));
		ArrayList<String> time_var_names = new ArrayList<String>();
		Integer idx = 1;
		format = "%s <- %s;\n";
		int count = 0;
		for (String time : times) {
			String time_var_name = getTimeVariableName(idx++);
			if (count > 0) fout.write(",");
			fout.write(String.format(format, time_var_name, time));
			time_var_names.add(time_var_name);
			count++;
		}
		
		// Write the catted array.
		format = "%ss = c(";
		fout.write(String.format(format, tspan_symbol));
		
		for (String time_var_name: time_var_names) fout.write(time_var_name + " ");
		fout.write(");\n");
		
		format = "%s <- sort(unique(%ss));\n";
		fout.write(String.format(format, tspan_symbol, tspan_symbol));
		
		format = "rm(%ss);\n";
		fout.write(String.format(format, tspan_symbol));
		
		format = "rm(%s)\n";
		for (String time_var_name: time_var_names) fout.write(String.format(format, time_var_name));
		
		format = "%s <- min(%s);\n"; 
		fout.write(String.format(format, tmin_symbol, tspan_symbol));
		
		format = "%s <- max(%s);\n\n"; 
		fout.write(String.format(format, tmax_symbol, tspan_symbol));
		
		writeTspan(fout);
	}
	
	protected void writeTspan(PrintWriter fout) {
		if (fout == null) return;
		
		String format = "%s = seq(%s, %s, length=%s);\n\n";
		fout.write(String.format(format, tspan_symbol, tmin_symbol, tmax_symbol, tspan_segments));
	}

	@Override
	public void writeInitialConditions(PrintWriter fout, StructuralBlock block) throws IOException {
		if (fout == null || block == null) return;
		
		StructuralModelType model = block.getModel();
		if (model == null) return;
		
		ArrayList<DerivativeVariableType> values = new ArrayList<DerivativeVariableType>();
		List<JAXBElement<? extends CommonVariableDefinitionType>> tags = model.getCommonVariable();
		for (JAXBElement<? extends CommonVariableDefinitionType> tag : tags) {
			if (tag == null) continue;
			Object value = tag.getValue();
			if (value == null) continue;
			if (value instanceof DerivativeVariableType) values.add((DerivativeVariableType) value);
		}
		
		if (values.isEmpty()) return;
		
		String format = "%s Initial conditions\n%s <- c(";
		fout.write(String.format(format, comment_char, initial_conditions_symbol));
		
		int count = 0;
		for (DerivativeVariableType value : values) {
			InitialConditionType ic = value.getInitialCondition();
			if (ic == null) throw new NullPointerException("Initial condition not specified");
			if (count > 0) fout.write(",");
			parse(ic, lexer.getTreeMaker().newInstance(ic), fout);
			count++;
		}
		
		SimulationStep step = lexer.getSimulationStep();
		StructuralModelType smt = block.getModel();
		if (step != null && block != null && smt != null) {
			if (step.hasContinuous()) {
				count = 0;
				boolean attachedPerStepValues = false;
				for (JAXBElement<? extends CommonVariableDefinitionType> var : smt.getCommonVariable()) {
					CommonVariableDefinitionType value = var.getValue();
					if (value instanceof VariableDefinitionType) {
						VariableDefinitionType local = (VariableDefinitionType) value;
						if (step.isPerStepVariable(local)) {
							if (!attachedPerStepValues) {
								fout.write(",");
								attachedPerStepValues = true;
							}
							if (count > 0) fout.write(",");
							fout.write(" 0.0 ");
							count++;
						}
					}
				}
			}
		} 
		fout.write(")\n");
	}

	@Override
	public void writeSimulation(PrintWriter fout, StructuralBlock sb) throws IOException {
		if (fout == null || sb == null) return;
		String format = "%s <- %s(func = %s, y = %s, parms = %s, times = %s)\n";
		
		writeSourceGeneratedFuncs(fout);
		fout.write(String.format(format, simulation_output_symbol, solver, getModelFunctionName(sb), initial_conditions_symbol, param_model_symbol, tspan_symbol));
	}

	@Override
	public void writeSimulationOptions(PrintWriter fout) throws IOException {
		if (fout == null) return;
		
		String format = "%s Desolve simulation options (if any).\n";
		fout.write(String.format(format, comment_char));
	}

	@Override
	public List<Artifact> writeContinuous(PrintWriter fout, SimulationStep step) {
		List<Artifact> artifacts = createArtifactList();
		if (fout == null || step == null) return artifacts;
		
		TreeMaker tm = lexer.getTreeMaker();
		Integer idx = 1;
		for (ObservationsType ob : step.getTimePoints().keySet()) {
			if (ob == null) continue;
			TimepointsType tps = ob.getTimepoints();
			if (tps == null) continue;
			JAXBElement<?> arrays = tps.getArrays();
			if (arrays == null) continue;
			Object value = arrays.getValue();
			if (value == null) continue;
			
			BinaryTree bt = null;
			if (isSequence(value)) bt = tm.newInstance((SequenceType) value);
			else if (isVector(value)) bt = tm.newInstance((VectorType) value);
			else throw new UnsupportedOperationException("Timepoints class not supported yet.");
			
			lexer.updateNestedTrees();
			
			String sampling_times = parse(tps, bt);
			String sampling_array_variable = getTimeVariableName(idx);
			
			ContinuousObservationType continuous = ob.getContinuous();
			if (continuous == null) continue;
			
			String format = null;
			if (lexer.isAtLastStructuralBlock()) {
				format = "\n%s = %s; %s Sampling array\n";
				fout.write(String.format(format, sampling_array_variable, sampling_times, comment_char));
			}
			
			for (SymbolRefType ref : continuous.getSymbRef()) {
				Artifact a = null;
				if (lexer.isMixedEffect()) 
					a = writeMixedEffectSimulationSampling(fout, idx, step, ref, sampling_array_variable);			
				else {
					if (lexer.hasTrialDesign() && lexer.hasDosing()) a = writePkPDSimulationWithDosingSampling(fout, step, ref, sampling_array_variable);
					else a = writePkPDSimulationSampling(fout, step, ref, sampling_array_variable);
				}
				if (isOkToAddToArtifacts(a, artifacts)) artifacts.add(a);
			}
			
			if (lexer.isMixedEffect()) 
				writeIndependentVariableMixedEffectOutputStructure(fout, idx, sampling_array_variable);
			else if (lexer.hasTrialDesign() && lexer.hasDosing()) 
				writeRenameSamplingIndependentVariable(fout, sampling_array_variable);
			else 
				writeRenameSamplingIndependentVariable(fout, sampling_array_variable);
			
			idx++;
		}
		
		return artifacts;
	}
	
	private void writeRenameSamplingIndependentVariable(PrintWriter fout, String sampling_array_variable) {
		if (fout == null || sampling_array_variable == null) return;
		
		String format = "%s <- %s;\n";
		String var_name = getSymbol(lexer.getDom().getIndependentVariable());
		fout.write(String.format(format, var_name, sampling_array_variable));
	}
	
	private void writeIndependentVariableMixedEffectOutputStructure(PrintWriter fout, Integer idx, String sampling_array_variable) {
		if (fout == null || idx == null) return;
		
		String struct_name = getMixedEffectSimulationStructName(idx);
		String independentVariable = getSymbol(lexer.getDom().getIndependentVariable().getSymbId());
		String	format = "%s.%s(%s,:) = %s;\n";
		fout.write(String.format(format, struct_name, independentVariable, REPLICATE_IDX_SYMBOL, sampling_array_variable));
	}
	
	private Artifact writeMixedEffectSimulationSampling(PrintWriter fout, Integer idx, SimulationStep step, SymbolRefType ref, String sampling_array_variable) {
		if (fout == null || step == null || ref == null || sampling_array_variable == null) return null;
		
		String struct_name = getMixedEffectSimulationStructName(idx);
		Artifact artefact = new Artifact(sampling_array_variable, struct_name, null);
		String independentVariable = getSymbol(lexer.getDom().getIndependentVariable().getSymbId());
		PharmMLRootType element = lexer.getAccessor().fetchElement(ref);
		
		if (lexer.isAtLastStructuralBlock()) {
			if (isDerivative(element)) {
				DerivativeVariableType dv = (DerivativeVariableType) element;
				String format = "%s.%s(%s,:) = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, struct_name, dv.getSymbId(), REPLICATE_IDX_SYMBOL, independentVariable, dv.getSymbId(), sampling_array_variable));
			} else if (isGeneralError(element)) {
				GeneralObsError goe = (GeneralObsError) element;
				String	format = "%s.%s(%s,:) = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, struct_name, goe.getSymbId(), REPLICATE_IDX_SYMBOL, independentVariable, goe.getSymbId(), sampling_array_variable));
			} else if (isGaussianError(element)) {
				GaussianObsError goe = (GaussianObsError) element;
				String	format = "%s.%s(%s,:) = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, struct_name, goe.getSymbId(), REPLICATE_IDX_SYMBOL, independentVariable, goe.getSymbId(), sampling_array_variable));
			} else if (isLocalVariable(element)) {
				VariableDefinitionType v = (VariableDefinitionType) element;
				if (step.isPerStepVariable(v)) {
					String	format = "%s.%s(%s,:) = interp1(%s,%s,%s);\n";
					fout.write(String.format(format, struct_name, v.getSymbId(), REPLICATE_IDX_SYMBOL, independentVariable, v.getSymbId(), sampling_array_variable));
				}
			} else 
				throw new UnsupportedOperationException("unsuported continuous type (" + element + ")");
		}
		
		return artefact;
	}
	
	private Artifact writePkPDSimulationWithDosingSampling(PrintWriter fout, SimulationStep step, SymbolRefType ref, String sampling_array_variable) {
		if (fout == null || step == null || ref == null || sampling_array_variable == null) return null;
		
		String independentVariable = getSymbol(lexer.getDom().getIndependentVariable().getSymbId());
		Artifact artefact = new Artifact(independentVariable, ref.getSymbIdRef(), sampling_array_variable);
		PharmMLRootType element = lexer.getAccessor().fetchElement(ref);
		
		if (lexer.isAtLastStructuralBlock()) {
			if (element instanceof DerivativeVariableType) {
				DerivativeVariableType dv = (DerivativeVariableType) element;
				String format = "%s = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, dv.getSymbId(), independentVariable, ref.getSymbIdRef(), sampling_array_variable, sampling_array_variable));
			} else if (element instanceof GeneralObsError) {
				GeneralObsError goe = (GeneralObsError) element;
				String	format = "%s = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, goe.getSymbId(), independentVariable, goe.getSymbId(), sampling_array_variable, sampling_array_variable));
			} else if (isGaussianError(element)) {
				GaussianObsError goe = (GaussianObsError) element;
				String format = "%s = interp1(%s,%s,%s);\n";
				fout.write(String.format(format, goe.getSymbId(), independentVariable, goe.getSymbId(), sampling_array_variable)); 
			}  else if (isLocalVariable(element)) {
				VariableDefinitionType v = (VariableDefinitionType) element;
				if (step.isPerStepVariable(v)) {
					String	format = "%s = interp1(%s,%s,%s);\n";
					fout.write(String.format(format,v.getSymbId(), independentVariable, ref.getSymbIdRef(), sampling_array_variable, sampling_array_variable));
				}
			}
			else 
				throw new UnsupportedOperationException("unsuported continuous type (" + element + ")");
		}
		
		return artefact;
	}
	
	// TODO: Work
	private Artifact writePkPDSimulationSampling(PrintWriter fout, SimulationStep step, SymbolRefType ref, String sampling_array_variable) {
		if (fout == null || step == null || ref == null || sampling_array_variable == null) return null;
		
		String independentVariable = getSymbol(lexer.getDom().getIndependentVariable());
		Artifact artifact = new Artifact(independentVariable, ref.getSymbIdRef(), sampling_array_variable);
		PharmMLRootType element = lexer.getAccessor().fetchElement(ref);
		
		if (lexer.isAtLastStructuralBlock()) {
			if (isDerivative(element)) {
				DerivativeVariableType dv = (DerivativeVariableType) element;
				Integer state_index = lexer.getStateVariableIndex(dv.getSymbId());
				if (state_index == -1) throw new IllegalStateException("State variable index unknown (var='" + dv.getSymbId()  + "')");
				String format = "%s = interp1(%s[,1],%s[,%s],xi=%s);\n";
				fout.write(String.format(format, dv.getSymbId(),simulation_output_symbol,simulation_output_symbol, ++state_index, sampling_array_variable));
			} else if (isGeneralError(element)) {
				System.err.println("INFO: Add code for general error model sampling (writePkPDSimulationSampling)");
				//GeneralObsError goe = (GeneralObsError) element;
				//String format = "%s = approx(%s[,1],y=%s[:,%s],%s);\n";
				//fout.write(String.format(format, goe.getSymbId(), independentVariable, goe.getSymbId(), sampling_array_variable));
			} else if (isGaussianError(element)) {
				System.err.println("INFO: Add code for gaussian error model sampling (writePkPDSimulationSampling)");
				//GaussianObsError goe = (GaussianObsError) element;
				//String format = "%s = interp1(%s,%s,%s);\n";
				//fout.write(String.format(format, goe.getSymbId(), independentVariable, goe.getSymbId(), sampling_array_variable)); 
			} else if (isLocalVariable(element)) {
				VariableDefinitionType v = (VariableDefinitionType) element;
				if (step.isPerStepVariable(v)) {
					Integer state_index = step.getPerStepVariableIndex(v);
					if (state_index == -1) throw new IllegalStateException("State variable index unknown (var='" + v.getSymbId()  + "')");
					String format = "%s = interp1(%s[,1],%s[,%s],xi=%s);\n";
					fout.write(String.format(format, v.getSymbId(),simulation_output_symbol,simulation_output_symbol,++state_index, sampling_array_variable));
				}
			} else 
				throw new UnsupportedOperationException("unsuported continuous type (" + element + ")");
		}
		
		return artifact;
	}

	@Override
	public String writeSaveCommand(PrintWriter fout, List<Artifact> refs, String output_dir, String id1, String id2) throws IOException {
		String filepath = null;
		
		String indepedent_variable_symbol = getSymbol(lexer.getDom().getIndependentVariable());
		if (indepedent_variable_symbol == null) throw new NullPointerException("Independent variable symbol is NULL.");
		
		if (fout != null && refs!= null && output_dir != null && id1 != null && id2 != null) {
			if (refs.isEmpty()) return null;
			
			filepath = getOutputFilepath(output_dir, id1, id2);
			filepath = filepath.replaceAll("\\\\", "/"); // 'R' specfic formatting, stupider than MATLAB/Octave
			
			Artifact iv_ref = null;
			
			// Find if independent variable specified.
			for (Artifact ref : refs) {
				if (ref == null) continue;
				String iv = ref.independentVariable;
				if (indepedent_variable_symbol.equals(iv)) {
					iv_ref = new Artifact(indepedent_variable_symbol, indepedent_variable_symbol, indepedent_variable_symbol);
					break;
				}
			}
			if (iv_ref != null) refs.add(0, iv_ref);
			
			String df_name = writeSaveDataFrameDeclaration(fout, id1, id2, refs);
			if (df_name == null) throw new NullPointerException("Dataframe name is NULL.");
			writeCSVSaveCommand(fout, df_name, filepath);
			outputs_to_csvfile_map.put(refs, filepath);
		}
		
		return filepath;
	}
	
	private void writeCSVSaveCommand(PrintWriter fout, String variable_name, String filepath) {
		if (fout == null) return;
		
		String format = "write.csv(%s, file=\"%s\", row.names=FALSE);\n";
		fout.write(String.format(format, variable_name, filepath));
		
	}
	
	private String getSaveDataFramename(String id1, String id2) {
		String format = "df_%s_%s"; 
		return String.format(format, id1, id2);
	}
	
	private String writeSaveDataFrameDeclaration(PrintWriter fout, String id1, String id2, List<Artifact> refs) {
		if (fout == null || id1 == null || id2 == null || refs == null) return null;
		if (refs.isEmpty()) return null;
		
		String dfname = getSaveDataFramename(id1, id2);
		
		String format = "\n%s <- data.frame(";
		fout.write(String.format(format, dfname));
		
		int i = 0;
		for (Artifact ref : refs) {
			if (ref == null) continue;
			if (i > 0) fout.write(",");
			fout.write(ref.outputVariable);
			i++;
		}
		
		fout.write(");\n\n");
		
		return dfname;
	}

	@Override
	public void writeArmLoop(PrintWriter fout, List<ArmIndividual> list) throws IOException {
	}

	@Override
	public void writeInitObservationBlockParameters(PrintWriter fout, List<ObservationBlock> obs) throws IOException {}

	@Override
	protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
		if (leaf == null) throw new NullPointerException("Tree leaf is NULL.");

		if (leaf.data != null) {
			boolean inPiecewise = false;
			if (isPiecewise(leaf.data)) inPiecewise = true;
			
			if (!isString_(leaf.data)) leaf.data = getSymbol(leaf.data);
			String current_value = "", current_symbol = "_FAKE_FAKE_FAKE_FAKE_";
			if (isDerivative(context) || isParameter(context) || isLocalVariable(context)) {
				String format = "(%s, FIX); %s\n", description = "";
				if (isRootType(context)) description = readDescription((PharmMLRootType) context);
				current_symbol = getSymbol(context);
				current_value = String.format(format, leaf.data, description);
			} else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
				String format = "(%s) ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isPiece(context)) { 
				String format = "%s ";
				current_value = String.format(format, (String) leaf.data);
			} else if (isContinuous(context)) {
				current_symbol = getSymbol(context);
				String format = "%s <- %s; \n";
				current_value = String.format(format, current_symbol, leaf.data);
			} else if (isActivity(context)) { 
				current_value = getSymbol(new ActivityDoseAmountBlock((ActivityType) context, (String) leaf.data));
			} else if (isIndividualParameter(context)) { 
				current_value = (String) leaf.data;
			} else if (isRandomVariable(context)) {
				ParameterRandomVariableType rv = (ParameterRandomVariableType) context;
				String format = "%s <- %s;\n";
				current_value = String.format(format, rv.getSymbId(), (String) leaf.data);
			} else if (isCovariate(context)) { 
				CovariateDefinitionType cov = (CovariateDefinitionType) context;
				String format = "%s <- %s;\n";
				current_value = String.format(format, cov.getSymbId(), (String) leaf.data);
			} else if (isObservationParameter(context)) {
				ObservationParameter op = (ObservationParameter) context;
				String format = "%s <- %s;\n";
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
	
	protected String doIndividualParameterAssignment(IndividualParameterType ip) {

    	StringBuilder stmt = new StringBuilder();
    	
    	String variableSymbol = ip.getSymbId();
    	
    	if (ip.getAssign() != null) {
    		stmt.append(String.format("%s = ", variableSymbol));
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
//    		else if (transform == LhsTransformationType.PROBIT) variableSymbol = "probit" + capitalise(variableSymbol);
    		
    		stmt.append(String.format("%s = ", variableSymbol));
    		
    		if (lcov != null) {
    			String pop_param_symbol = null;
    			
    			PopulationParameter pop_param = lcov.getPopulationParameter();
    			if (pop_param != null) {
    				pop_param_symbol = parse(pop_param, lexer.getStatement(pop_param));
    				
    				if (transform == LhsTransformationType.LOG) pop_param_symbol = String.format("log(%s)", pop_param_symbol);
    	    		else if (transform == LhsTransformationType.LOGIT) pop_param_symbol = String.format("logit(%s)", pop_param_symbol);
//    	    		else if (transform == LhsTransformationType.PROBIT) pop_param_symbol = String.format("probit(%s)", pop_param_symbol);
    				
    				stmt.append(String.format("(%s))", pop_param_symbol));
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
    									String fixed_effect_stmt = parse(fixed_effect, lexer.getStatement(fixed_effect));
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
    		
    		int nRandomEffects = 0;
			if (random_effects != null) {
				if (!random_effects.isEmpty()) {
					if (nCovs > 0) stmt.append(" + ");
					for (ParameterRandomEffectType random_effect : random_effects) {
						if (random_effect == null) continue;
						if (nRandomEffects > 0) stmt.append(" + ");
						stmt.append(parse(random_effect, lexer.getStatement(random_effect)));
						nRandomEffects++;
					}
				}
			}
			stmt.append(";\n");
			
			if (transform == LhsTransformationType.LOG) {
				String format = "%s = EXP(%s);\n";
				stmt.append(String.format(format, ip.getSymbId(), variableSymbol));
			} else if (transform == LhsTransformationType.LOGIT) {
				String format = "%s = 1./(1 + exp(-%s));\n";
				stmt.append(String.format(format, ip.getSymbId(), variableSymbol));
			}
    	}
		stmt.append("\n");
		
		return stmt.toString();
	
		
	}

	@Override
	public void writeVariableAssignments(PrintWriter fout, SimulationStep step) throws IOException {
	}

	@Override
	public void writeErrorModel(PrintWriter fout, List<ObservationBlock> ems) throws IOException {
	}

	@Override
	public void writePlottingBlockPkPdSimulationWithDosing(PrintWriter fout)
			throws IOException {
	}
	
	@Override
	public String getScriptFilename(String output_dir) {
		String format = "%s/%s.%s";
		return String.format(format, output_dir, run_id, script_file_suffix);
	}
	
	@Override
	public void writePurgeCommand(PrintWriter fout) {
		if (fout == null) return;
		
		fout.write("rm(list=ls());\n\n");
	}
	
	@Override
	public void writeExternallyReferencedElement(PrintWriter fout, StructuralBlock sb) throws IOException {
	}
}
