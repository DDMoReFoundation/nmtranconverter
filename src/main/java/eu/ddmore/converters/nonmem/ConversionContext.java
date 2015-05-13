/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.spi.ILexer;
import crx.converter.spi.IParser;
import crx.converter.tree.BinaryTree;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.PredStatement;
import eu.ddmore.converters.nonmem.statements.SigmaStatementBuilder;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.OrderedEtasHandler;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ErrorModel;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ResidualError;

/**
 * Conversion context class accesses common converter for nmtran conversion and initialises 
 * required information for block by block conversion.
 *  
 */
public class ConversionContext {

    private final IParser parser;
    private final ILexer lexer;
    private final ParametersHelper parameterHelper;
    private List<String> thetas = new ArrayList<String>();
    private Map<String, Integer> orderedEtas = new LinkedHashMap<String, Integer>();
    private List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();

    ConversionContext(IParser parser, ILexer lexer) throws IOException{
        this.parser = parser;
        this.lexer = lexer;

        this.parameterHelper = new ParametersHelper(lexer.getScriptDefinition());

        initialise();
    }

    /**
     * This method will initialise parameters, eta order, theta assignments and error statements, 
     * which will be required for nmtran block translations.
     *  
     * @throws IOException
     */
    public void initialise() throws IOException{
        //initialise parameters
        if (lexer.getModelParameters().isEmpty()) {
            throw new IllegalArgumentException("Cannot find simple parameters for the pharmML file.");
        }
        OrderedEtasHandler etasHandler = new OrderedEtasHandler(getScriptDefinition()); 
        //set etas order which will be referred by most of the blocks
        setOrderedEtas(etasHandler.createOrderedEtasMap());
        parameterHelper.initialiseAllParameters(lexer.getModelParameters());
        setThetaAssigments();

        // Initialise error statement
        errorStatements = prepareAllErrorStatements();
    }

    /**
     * Builds and writes pred statement block to file.
     *  
     * @param fout
     * @return 
     */
    public StringBuilder buildPredStatement(){
        PredStatement predStatement = new PredStatement(this);
        return predStatement.getPredStatement();
    }

    /**
     * This method will get parameter blocks and add it to parameter statement. 
     * @return
     */
    public StringBuilder getParameterStatement() {
        StringBuilder parameterStatement = new StringBuilder();

        StringBuilder thetaStatement = parameterHelper.getThetaStatementBlock();
        parameterStatement.append(thetaStatement.toString());

        StringBuilder omegaStatement = parameterHelper.getOmegaStatementBlock();
        parameterStatement.append(omegaStatement.toString());

        SigmaStatementBuilder sigmaBuilder = new SigmaStatementBuilder(parameterHelper);
        StringBuilder sigmaStatement = sigmaBuilder.getSigmaStatementBlock();
        parameterStatement.append(sigmaStatement.toString());
        return parameterStatement;
    }

    /**
     * Set theta assignements from theta parameters generated.
     */
    private void setThetaAssigments(){
        thetas.addAll(parameterHelper.getThetaParams().keySet());
    }

    /**
     * This method will build theta assignment statements
     * @param fout
     */
    public StringBuilder buildThetaAssignments() {
        StringBuilder thetaAssignmentBlock = new StringBuilder();  
        for(String theta : parameterHelper.getThetaParams().keySet()){
            thetaAssignmentBlock.append(Formatter.endline(theta+ " = "+getThetaForSymbol(theta)));
        }
        return thetaAssignmentBlock;
    }

    /**
     * This method will get theta format for symbol. 
     *  
     * @param symbol
     * @return
     */
    private String getThetaForSymbol(String symbol){
        if(thetas.isEmpty()){
            setThetaAssigments();
        }
        if(thetas.contains(symbol)){
            symbol = String.format(Block.THETA+"(%s)",thetas.indexOf(symbol)+1);
        }
        return symbol;
    }

    /**
     * This method will build eta assignment statements to be displayed after theta assignments.
     * @return
     */
    public StringBuilder buildEtaAssignments() {
        StringBuilder etaAssignment = new StringBuilder();  
        for(String eta : getOrderedEtas().keySet()){
            etaAssignment.append(Formatter.endline(eta+ " = ETA("+getOrderedEtas().get(eta)+")"));
        }
        return etaAssignment;
    }

    /**
     * This method will list prepare all the error statements and returns the list.
     * We need to prepare this list separately as we need to use it in DES block before writing out to ERROR block.
     * @return
     */
    private List<ErrorStatement> prepareAllErrorStatements(){
        List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();

        for(ObservationBlock block : lexer.getScriptDefinition().getObservationBlocks()){
            ObservationError errorType = block.getObservationError();
            if(errorType instanceof GeneralObsError){
                //              GeneralObsError genError = (GeneralObsError) errorType;
                //              TODO : DDMORE-1013 : add support for general observation error type once details are available
                //              throw new IllegalArgumentException("general observation error type is not yet supported.");
            }
            if(errorType instanceof GaussianObsError){
                GaussianObsError error = (GaussianObsError) errorType;
                ErrorStatement errorStatement = prepareErrorStatement(error);
                errorStatements.add(errorStatement);
            }else{
                //              TODO : Check if there are any other types to encounter
            }
        }
        return errorStatements;
    }

    public static Set<ParameterRandomVariable> getEpsilonRandomVariables(ScriptDefinition scriptDefinition) {
        Set<ParameterRandomVariable> epsilonRandomVariables = new HashSet<>();
        List<ResidualError> residualErrors = retrieveResidualErrors(scriptDefinition);

        for(ParameterBlock paramBlock: scriptDefinition.getParameterBlocks()){
            if(residualErrors.isEmpty() || paramBlock.getRandomVariables().isEmpty()){
                break;
            }
            for(ResidualError error : residualErrors){
                String errorName = error.getSymbRef().getSymbIdRef();
                for(ParameterRandomVariable randomVar : paramBlock.getRandomVariables()){
                    if(randomVar.getSymbId().equals(errorName)){
                        epsilonRandomVariables.add(randomVar);
                    }
                }
            }
        }
        return epsilonRandomVariables;
    }

    private static List<ResidualError> retrieveResidualErrors(ScriptDefinition scriptDefinition){
        List<ResidualError> residualErrors = new ArrayList<>() ;
        for(ObservationBlock block : scriptDefinition.getObservationBlocks()){
            ObservationError errorType = block.getObservationError();
            if(errorType instanceof GaussianObsError){
                GaussianObsError error = (GaussianObsError) errorType;
                if(error.getResidualError()!=null){
                    residualErrors.add(error.getResidualError());
                }
            }
        }
        return residualErrors;
    }

    /**
     * This method will create scalar Rhs object for a symbol from the scalar value provided.
     *  
     * @param symbol
     * @param scalar
     * @return ScalarRhs object
     */
    public static ScalarRhs createScalarRhs(String symbol,JAXBElement<?> scalar) {
        ScalarRhs scalarRhs = new ScalarRhs();
        scalarRhs.setScalar(scalar);
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);
        scalarRhs.setSymbRef(symbRef);
        return scalarRhs;
    }

    /**
     * Prepares and returns error statement for the gaussian observation error.
     * 
     * @param error
     * @return
     */
    private ErrorStatement prepareErrorStatement(GaussianObsError error) {
        ErrorModel errorModel = error.getErrorModel();
        String output = error.getOutput().getSymbRef().getSymbIdRef();
        FunctionCallType functionCall = errorModel.getAssign().getEquation().getFunctionCall();

        ErrorStatement errorStatement = new ErrorStatement(functionCall, output);
        return errorStatement;
    }

    /**
     * It will parse the object passed and returns parsed results in form of string.
     *  
     * @param context
     * @return
     */
    public String parse(Object context){
        return parse(context, lexer.getStatement(context));
    }

    /**
     * It will parse the object passed with help of binary tree provided.
     * This tree is generated by common converter for elements.
     * 
     * @param context
     * @return
     */
    public String parse(Object context, BinaryTree tree){
        return parser.parse(context, tree);
    }

    public ScriptDefinition getScriptDefinition(){
        return lexer.getScriptDefinition();    
    }

    public Map<String, Integer> getOrderedEtas() {
        return orderedEtas;
    }

    public void setOrderedEtas(Map<String, Integer> etasOrder) {
        this.orderedEtas = etasOrder;
    }

    public ParametersHelper getParameterHelper() {
        return parameterHelper;
    }

    public List<ErrorStatement> getErrorStatements() {
        return errorStatements;
    }

    public IParser getParser() {
        return parser;
    }

    public ILexer getLexer() {
        return lexer;
    }
}
