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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.ILexer;
import crx.converter.spi.IParser;
import crx.converter.spi.blocks.StructuralBlock;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.CorrelationHandler;
import eu.ddmore.converters.nonmem.parameters.ParametersBuilder;
import eu.ddmore.converters.nonmem.parameters.ParametersInitialiser;
import eu.ddmore.converters.nonmem.statements.DataSetHandler;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationDetailsEmitter;
import eu.ddmore.converters.nonmem.statements.input.InputColumnsHandler;
import eu.ddmore.converters.nonmem.statements.input.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.statements.model.ModelStatementHelper;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.MuReferenceHandler;
import eu.ddmore.converters.nonmem.utils.OrderedThetasHandler;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.DepotMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;
import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;

/**
 * Conversion context class accesses common converter for nmtran conversion and initialises 
 * required information for block by block conversion.
 *  
 */
public class ConversionContext {

    private final IParser parser;
    private final ILexer lexer;
    private final LocalParserHelper localParserHelper;
    private final ParametersBuilder parametersBuilder;
    private final OrderedThetasHandler orderedThetasHandler;
    private final DiscreteHandler discreteHandler;
    private final CorrelationHandler correlationHandler;
    private final List<DerivativeVariable> derivativeVars = new ArrayList<DerivativeVariable>();
    private final List<DerivativeVariable> nonPkMacroDerivativeVars = new ArrayList<DerivativeVariable>();
    private final List<DerivativeVariable> pkMacroDerivativeVars = new ArrayList<DerivativeVariable>();
    private final Map<String, String> derivativeVarCompSequences = new LinkedHashMap<String, String>();
    private final ConditionalEventHandler conditionalEventHandler;
    private final MuReferenceHandler muReferenceHandler;
    private final EstimationDetailsEmitter estimationEmitter;
    private final InterOccVariabilityHandler iovHandler; 
    private final InputColumnsHandler inputColumnsHandler;
    private final DataSetHandler dataSetHandler;
    private final ParametersInitialiser parameterInitialiser;
    private final ModelStatementHelper modelStatementHelper;

    ConversionContext(File srcFile, IParser parser, ILexer lexer) throws IOException{
        Preconditions.checkNotNull(srcFile, "source file cannot be null");
        Preconditions.checkNotNull(parser, " common converter parser cannot be null");
        Preconditions.checkNotNull(lexer, "common converter lexer cannot be null");

        this.parser = parser;
        this.lexer = lexer;

        lexer.setFilterReservedWords(true);

        parser.getSymbolReader().loadReservedWords();
        localParserHelper = new LocalParserHelper(this);

        //This sequence of initialisation is important for information availability.  
        this.inputColumnsHandler = new InputColumnsHandler(retrieveExternalDataSets(),lexer.getCovariates());
        String dataLocation = srcFile.getAbsoluteFile().getParentFile().getAbsolutePath();
        this.dataSetHandler = new DataSetHandler(retrieveExternalDataSets(), dataLocation);
        this.orderedThetasHandler = new OrderedThetasHandler(this);
        this.iovHandler = new InterOccVariabilityHandler(this);
        this.correlationHandler = new CorrelationHandler(this);
        this.discreteHandler = new DiscreteHandler(getScriptDefinition());
        //Refers to discrete handler
        this.estimationEmitter = new EstimationDetailsEmitter(getScriptDefinition(), discreteHandler);
        //initialise parameters
        this.parameterInitialiser = initialisePopulationParams(lexer.getModelParameters());
        this.parametersBuilder = new ParametersBuilder(this);

        this.conditionalEventHandler = new ConditionalEventHandler(this);
        this.muReferenceHandler = new MuReferenceHandler(this);
        initialise();
        this.modelStatementHelper = new ModelStatementHelper(this);
    }

    /**
     * This method will initialise parameters, eta order, theta assignments and error statements, 
     * which will be required for nmtran block translations.
     * @throws IOException 
     */
    private void initialise() throws IOException{

        orderedThetasHandler.createOrderedThetasToEta(retrieveOrderedEtas());
        parametersBuilder.initialiseAllParameters();

        for(DerivativeVariable derivativeVar : getAllStateVariables()){
            if(derivativeVar.isOriginatedFromMacro()){
                pkMacroDerivativeVars.add(derivativeVar);
            }else{
                nonPkMacroDerivativeVars.add(derivativeVar);
            }
        }

        setDerivativeVarCompartmentSequence();
    }

    private ParametersInitialiser initialisePopulationParams(List<PopulationParameter> populationParameters) {
        if (populationParameters==null || populationParameters.isEmpty()) {
            throw new IllegalArgumentException("Cannot find simple parameters for the pharmML file.");
        }else{
            return new ParametersInitialiser(populationParameters, getScriptDefinition());
        }
    }

    /**
     * This method will get parameter blocks and add it to parameter statement. 
     */
    public StringBuilder getParameterStatement() {
        StringBuilder parameterStatement = new StringBuilder();

        String thetaStatement = parametersBuilder.getThetasBuilder().getThetaStatementBlock();
        parameterStatement.append(thetaStatement);

        String omegaStatement = parametersBuilder.getOmegasBuilder().getOmegaStatementBlock();
        if(!omegaStatement.isEmpty()){
            parameterStatement.append(omegaStatement);
        }

        String omegaStatementForIOV = parametersBuilder.getOmegasBuilder().getOmegaStatementBlockForIOV();
        if(!omegaStatementForIOV.isEmpty()){
            parameterStatement.append(omegaStatementForIOV);
        }

        //adding default Omega if omega block is absent  
        if(!(isOmegaForIIVPresent() || isOmegaForIOVPresent())){
            parameterStatement.append(Formatter.endline());
            parameterStatement.append(Formatter.endline(Formatter.omega()+"0 "+NmConstant.FIX));
        }

        StringBuilder sigmaStatement = parametersBuilder.getSigmasBuilder().getSigmaStatementBlock();
        parameterStatement.append(sigmaStatement);

        return parameterStatement;
    }

    /**
     * Check and returns true if sigma statement is present
     */
    public boolean isSigmaPresent(){
        StringBuilder sigmaStement = parametersBuilder.getSigmasBuilder().getSigmaStatementBlock();
        return !sigmaStement.toString().trim().isEmpty();
    }

    /**
     * Check and returns true if omega statement for IIV is present
     */
    public boolean isOmegaForIIVPresent(){
        return !parametersBuilder.getOmegasBuilder().getOmegaStatementBlock().trim().isEmpty();
    }

    /**
     * Check and returns true if omega statement for IOV is present
     */
    public boolean isOmegaForIOVPresent(){
        return !parametersBuilder.getOmegasBuilder().getOmegaStatementBlockForIOV().trim().isEmpty();
    }

    private Map<String, String> setDerivativeVarCompartmentSequence(){

        List<DerivativeVariable> absMacroDerivativeVars = new ArrayList<>();
        List<DerivativeVariable> cmtMacroDerivativeVars = new ArrayList<>();
        List<DerivativeVariable> perifMacroDerivativeVars = new ArrayList<>();
        List<DerivativeVariable> depotMacroDerivativeVars = new ArrayList<>();

        for(DerivativeVariable derivativeVar : pkMacroDerivativeVars){
            if(derivativeVar.getOriginMacro() instanceof AbsorptionOralMacro){
                absMacroDerivativeVars.add(derivativeVar);
            }else if(derivativeVar.getOriginMacro() instanceof DepotMacro){
                depotMacroDerivativeVars.add(derivativeVar);
            }else if(derivativeVar.getOriginMacro() instanceof CompartmentMacro){
                cmtMacroDerivativeVars.add(derivativeVar);
            }else if(derivativeVar.getOriginMacro() instanceof PeripheralMacro){
                perifMacroDerivativeVars.add(derivativeVar);
            }
        }

        int i=1;
        i = addDerivativeVarsToCompSequence(absMacroDerivativeVars, i);
        i = addDerivativeVarsToCompSequence(depotMacroDerivativeVars, i);
        i = addDerivativeVarsToCompSequence(cmtMacroDerivativeVars, i);
        i = addDerivativeVarsToCompSequence(perifMacroDerivativeVars, i);

        addDerivativeVarsToCompSequence(nonPkMacroDerivativeVars, i);

        return derivativeVarCompSequences;
    }

    private int addDerivativeVarsToCompSequence(List<DerivativeVariable> derivativeVarsToAdd, int i) {
        for(DerivativeVariable derivativeVar : derivativeVarsToAdd){
            derivativeVars.add(derivativeVar);
            derivativeVarCompSequences.put(derivativeVar.getSymbId().toUpperCase(), Integer.toString(i++));
        }
        return i;
    }

    /**
     * Collects all derivativeVariable types (state variables) from structural blocks in order to create model statement.
     */
    private Set<DerivativeVariable> getAllStateVariables() {
        Set<DerivativeVariable> stateVariables = new LinkedHashSet<DerivativeVariable>();
        for(StructuralBlock structuralBlock : getScriptDefinition().getStructuralBlocks() ){
            stateVariables.addAll(structuralBlock.getStateVariables());
        }
        return stateVariables;
    }

    /**
     * Get external dataSets from list of data files 
     */
    public List<ExternalDataSet> retrieveExternalDataSets(){
        return lexer.getDataFiles().getExternalDataSets();
    }

    public Map<String, String> getReservedWords(){
        return parser.getSymbolReader().getReservedWordMap();
    }

    public ScriptDefinition getScriptDefinition(){
        return lexer.getScriptDefinition();
    }

    public ParametersBuilder getParametersBuilder() {
        return parametersBuilder;
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

    public Set<Eta> retrieveOrderedEtas() {
        return correlationHandler.getAllOrderedEtas();
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

    public CorrelationHandler getCorrelationHandler() {
        return correlationHandler;
    }

    public ConditionalEventHandler getConditionalEventHandler() {
        return conditionalEventHandler;
    }

    public MuReferenceHandler getMuReferenceHandler() {
        return muReferenceHandler;
    }

    public InterOccVariabilityHandler getIovHandler() {
        return iovHandler;
    }

    public InputColumnsHandler getInputColumnsHandler() {
        return inputColumnsHandler;
    }

    public DataSetHandler getDataSetHandler() {
        return dataSetHandler;
    }

    public EstimationDetailsEmitter getEstimationEmitter() {
        return estimationEmitter;
    }

    public LocalParserHelper getLocalParserHelper() {
        return localParserHelper;
    }

    public ParametersInitialiser getParameterInitialiser() {
        return parameterInitialiser;
    }

    public List<ColumnMapping> getColumnMappings() {
        return dataSetHandler.getColumnMappings();
    }

    public ModelStatementHelper getModelStatementHelper() {
        return modelStatementHelper;
    }

    public List<DerivativeVariable> getNonPkMacroDerivativeVars() {
        return nonPkMacroDerivativeVars;
    }

    public List<DerivativeVariable> getPkMacroDerivativeVars() {
        return pkMacroDerivativeVars;
    }
}
