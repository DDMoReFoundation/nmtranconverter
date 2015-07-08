/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;
import crx.converter.engine.parts.ParameterBlock.Event;
import crx.converter.spi.ILexer;
import crx.converter.spi.IParser;
import crx.converter.tree.BinaryTree;
import eu.ddmore.converters.nonmem.statements.DiscreteHandler;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.PredStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.converters.nonmem.utils.OrderedEtasHandler;
import eu.ddmore.converters.nonmem.utils.OrderedThetasHandler;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ErrorModel;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

/**
 * Conversion context class accesses common converter for nmtran conversion and initialises 
 * required information for block by block conversion.
 *  
 */
public class ConversionContext {

    private final IParser parser;
    private final ILexer lexer;
    private final ParametersHelper parameterHelper;
    private final OrderedThetasHandler orderedThetasHandler;
    private final DiscreteHandler discreteHandler;
    private final OrderedEtasHandler etasHandler;
    private final List<String> thetas = new ArrayList<String>();
    private final List<ErrorStatement> errorStatements = new ArrayList<ErrorStatement>();
    private final List<DerivativeVariable> derivativeVars = new ArrayList<DerivativeVariable>();
    private final Map<String, String> derivativeVarCompSequences = new HashMap<String, String>();

    ConversionContext(IParser parser, ILexer lexer) throws IOException{
        this.parser = parser;
        this.lexer = lexer;

        this.orderedThetasHandler = new OrderedThetasHandler(getScriptDefinition());
        this.etasHandler = new OrderedEtasHandler(getScriptDefinition());
        this.discreteHandler = new DiscreteHandler(getScriptDefinition());
        this.parameterHelper = new ParametersHelper(getScriptDefinition(), etasHandler, orderedThetasHandler);
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
        orderedThetasHandler.createOrderedThetasToEta(retrieveOrderedEtas());
        parameterHelper.initialiseAllParameters(lexer.getModelParameters());
        setThetaAssigments();

        derivativeVars.addAll(getAllStateVariables());
        setDerivativeVarCompartmentSequence();

        // Initialise error statement
        prepareAllErrorStatements();
    }

    /**
     * Builds and writes pred statement block to file.
     *  
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

        StringBuilder sigmaStatement = parameterHelper.getSigmaStatementBlock();
        parameterStatement.append(sigmaStatement.toString());

        return parameterStatement;
    }

    /**
     * This method will build simple parameter assignment statements
     * @return
     */
    public StringBuilder getSimpleParamAssignments() {
        StringBuilder simpleParamAssignmentBlock = new StringBuilder();
        Map<String, SimpleParameter> params = parameterHelper.getSimpleParamsWithAssignment();

        for(String simpleParamSymbol : params.keySet()){
            SimpleParameter simpleParam = params.get(simpleParamSymbol);
            Event event = getEventForParameter(simpleParam);
            String parsedEquation = new String();
            if(event !=null){
                if(event.getPiecewiseTree()!=null && event.getPiecewiseTree().size()>0){
                    parsedEquation = parse(event.getParameter(), event.getPiecewiseTree());
                }
            }else {
                Equation simpleParamAssignmentEq = simpleParam.getAssign().getEquation();
                parsedEquation = simpleParamSymbol+ " = "+ getParser().getSymbol(simpleParamAssignmentEq);
            }

            if(!parsedEquation.isEmpty()){
                simpleParamAssignmentBlock.append(Formatter.endline(parsedEquation));
            }
        }
        return simpleParamAssignmentBlock;
    }

    /**
     * Get event for parameter from parameter block. 
     *  
     * @param param
     * @return
     */
    private Event getEventForParameter(SimpleParameter param){
        for(ParameterBlock pb : getScriptDefinition().getParameterBlocks()){
            if (pb.hasEvents()) {
                for (Event event : pb.getEvents()) {
                    if (event != null) {
                        if (param.getSymbId().equals(event.getParameter().getSymbId())){
                            return event;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set theta assignements from theta parameters generated.
     */
    private void setThetaAssigments(){
        thetas.addAll(parameterHelper.getThetaParams().keySet());
    }

    /**
     * This method will build theta assignment statements
     * @return
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
        Map<String, Integer> orderedThetas = retrieveOrderedEtas();
        for(String eta : orderedThetas.keySet()){
            etaAssignment.append(Formatter.endline(eta+ " = ETA("+orderedThetas.get(eta)+")"));
        }
        return etaAssignment;
    }

    /**
     * This method will list prepare all the error statements and returns the list.
     * We need to prepare this list separately as we need to use it in DES block before writing out to ERROR block.
     * @return
     */
    private List<ErrorStatement> prepareAllErrorStatements(){

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

    private Map<String, String> setDerivativeVarCompartmentSequence(){
        int i=1;
        for (DerivativeVariable variableType : derivativeVars){
            String variable = Formatter.addPrefix(variableType.getSymbId());
            derivativeVarCompSequences.put(variable, Integer.toString(i++));
        }
        return derivativeVarCompSequences;
    }

    /**
     * Collects all derivativeVariable types (state variables) from structural blocks in order to create model statement.
     * 
     * @return
     */
    private Set<DerivativeVariable> getAllStateVariables() {
        Set<DerivativeVariable> stateVariables = new LinkedHashSet<DerivativeVariable>();
        for(StructuralBlock structuralBlock : getScriptDefinition().getStructuralBlocks() ){
            stateVariables.addAll(structuralBlock.getStateVariables());
        }
        return stateVariables;
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
     * Get external dataSets from list of data files 
     * @return
     */
    public List<ExternalDataSet> retrieveExternalDataSets(){
        return getLexer().getDataFiles().getExternalDataSets();
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

    public OrderedThetasHandler getOrderedThetasHandler(){
        return orderedThetasHandler;
    }

    public Map<String, Integer> retrieveOrderedEtas() {
        return etasHandler.getOrderedEtas();
    }

    public List<DerivativeVariable> getDerivativeVars() {
        return derivativeVars;
    }

    public Map<String, String> getDerivativeVarCompSequences() {
        return derivativeVarCompSequences;
    }

    public DiscreteHandler getDiscreteHandler() {
        return discreteHandler;
    }


    public OrderedEtasHandler getEtasHandler() {
        return etasHandler;
    }
}
