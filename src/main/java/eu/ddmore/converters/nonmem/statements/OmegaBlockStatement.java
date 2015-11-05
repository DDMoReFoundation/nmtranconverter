/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.OrderedEtasHandler;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.ObjectFactory;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

public class OmegaBlockStatement {

    private static final String EMPTY_VARIABLE = "Empty Variable";
    private final Map<String, List<OmegaStatement>> omegaBlocksInNonIOV = new HashMap<String, List<OmegaStatement>>();
    private final Map<String, List<OmegaStatement>> omegaBlocksInIOV = new HashMap<String, List<OmegaStatement>>();

    private String omegaBlockTitleInIOV;
    private String omegaBlockTitleInNonIOV;
    private Boolean isOmegaBlockFromStdDev = false;
    private final ParametersHelper paramHelper;

    private Map<String, Integer> etaToOmagasInIOV = new HashMap<String, Integer>();
    private Map<String, Integer> etaToOmagasInNonIOV = new HashMap<String, Integer>();

    private Map<Integer, String> omegaOrderToEtasInIOV = new TreeMap<Integer, String>();
    private Map<Integer, String> omegaOrderToEtasInNonIOV = new TreeMap<Integer, String>();

    private final List<CorrelationRef> iovCorrelations = new ArrayList<CorrelationRef>();
    private final List<CorrelationRef> nonIovCorrelations = new ArrayList<CorrelationRef>();

    private final Set<String> etasInCorrelation;
    private final Set<String> etasInIOV;

    public OmegaBlockStatement(ParametersHelper parameter, OrderedEtasHandler orderedEtasHandler) {
        Preconditions.checkNotNull(parameter, "parameter should not be null");
        Preconditions.checkNotNull(orderedEtasHandler, "ordered etas handler should not be null");
        this.paramHelper = parameter;
        etasInCorrelation = orderedEtasHandler.getEtasToOmegasInCorrelation().keySet();
        etasInIOV = orderedEtasHandler.getEtasToOmegasInIOV().keySet();
        setEtaToOmagasInIOV(orderedEtasHandler.getOrderedEtasInIOV());
        setEtaToOmagasInNonIOV(orderedEtasHandler.getOrderedEtasInNonIOV());
        getAllCorrelations();
    }

    /**
     * This method will create omega blocks if there are any.
     * We will need ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     *  
     */
    public void createOmegaBlocks(){

        setOmegaBlockTitleInNonIOV(createOmegaBlockTitle(nonIovCorrelations, etaToOmagasInNonIOV));
        initialiseOmegaBlocks(nonIovCorrelations, omegaBlocksInNonIOV, etaToOmagasInNonIOV, etasInCorrelation);
        omegaOrderToEtasInNonIOV = reverseMap(etaToOmagasInNonIOV);
        createOmegaBlocks(nonIovCorrelations, omegaBlocksInNonIOV, etaToOmagasInNonIOV, 
            omegaOrderToEtasInNonIOV, etasInCorrelation);

        setOmegaBlockTitleInIOV(createOmegaBlockTitle(iovCorrelations, etaToOmagasInIOV));
        initialiseOmegaBlocks(iovCorrelations, omegaBlocksInIOV, etaToOmagasInIOV, etasInIOV);
        omegaOrderToEtasInIOV = reverseMap(etaToOmagasInIOV);
        createOmegaBlocks(iovCorrelations, omegaBlocksInIOV,etaToOmagasInIOV, 
            omegaOrderToEtasInIOV, etasInIOV);
    }

    private void createOmegaBlocks(List<CorrelationRef> correlations, Map<String, List<OmegaStatement>> omegaBlocks, 
            Map<String, Integer> etaToOmagas, Map<Integer, String> omegaOrderToEtas, Set<String> etas){

        if(!correlations.isEmpty()){

            for(String eta : omegaOrderToEtas.values()){
                Iterator<CorrelationRef> iterator = correlations.iterator();

                while(iterator.hasNext()){

                    CorrelationRef correlation = iterator.next();
                    ParameterRandomVariable firstRandomVar = correlation.rnd1;
                    ParameterRandomVariable secondRandomVar = correlation.rnd2;

                    // row = i column = j
                    int column = getOrderedEtaIndex(firstRandomVar.getSymbId(), etaToOmagas);
                    int row = getOrderedEtaIndex(secondRandomVar.getSymbId(), etaToOmagas);

                    if(column > row){
                        firstRandomVar = correlation.rnd2;
                        secondRandomVar = correlation.rnd1;
                        int swap = column;
                        column = row;
                        row = swap;
                    }

                    createFirstCorrMatrixRow(eta, firstRandomVar, omegaBlocks, etaToOmagas);

                    addVarToCorrMatrix(firstRandomVar, column, omegaBlocks);
                    addVarToCorrMatrix(secondRandomVar, row, omegaBlocks);

                    addCoefficientToCorrMatrix(correlation, omegaBlocks, firstRandomVar, secondRandomVar, column);

                    iterator.remove();
                }
            }
        }
    }

    /**
     * Creates first matrix row which will have only first omega as element.
     * 
     * @param eta
     * @param randomVar1
     * @param omegaBlocks
     * @param etaToOmagas
     */
    private void createFirstCorrMatrixRow(String eta, ParameterRandomVariable randomVar1, 
            Map<String, List<OmegaStatement>> omegaBlocks, Map<String, Integer> etaToOmagas) {
        if(etaToOmagas.get(randomVar1.getSymbId())== 1 && eta.equals(randomVar1.getSymbId())){
            List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
            String firstVariable = RandomVariableHelper.getNameFromParamRandomVariable(randomVar1);
            matrixRow.add(paramHelper.getOmegaFromRandomVarName(firstVariable));
            omegaBlocks.put(randomVar1.getSymbId(), matrixRow);
        }
    }

    /**
     * This method adds coefficient associated with random var1 and random var2 at [i,j] 
     * which is mirrored with [j,i] ([j,i] can be empty as its not required) from correlation or covariance provided.
     * If nothing is specified then default value will be specified. 
     * 
     * @param corr
     * @param omegaBlocks 
     * @param firstRandomVar
     * @param secondRandomVar
     * @param column
     */
    private void addCoefficientToCorrMatrix(CorrelationRef corr, Map<String, List<OmegaStatement>> omegaBlocks,
            ParameterRandomVariable firstRandomVar, ParameterRandomVariable secondRandomVar, int column) {
        List<OmegaStatement> omegas = omegaBlocks.get(secondRandomVar.getSymbId());
        if(omegas.get(column)==null || omegas.get(column).getSymbId().equals(EMPTY_VARIABLE)){
            OmegaStatement omega = null ;
            if(corr.isCorrelation()){
                omega = getOmegaForCoefficient(corr.correlationCoefficient, firstRandomVar, secondRandomVar);
            }else if(corr.isCovariance()){
                omega = getOmegaForCoefficient(corr.covariance, firstRandomVar, secondRandomVar);
            }
            if(omega != null){
                omegas.set(column,omega);
            }
        }
    }

    private void addVarToCorrMatrix(ParameterRandomVariable randomVar, int column, Map<String, List<OmegaStatement>> omegaBlocks) {
        List<OmegaStatement> omegas = omegaBlocks.get(randomVar.getSymbId());
        if(omegas.get(column)==null){
            initialiseRowElements(column, omegas);
            String secondVariable = RandomVariableHelper.getNameFromParamRandomVariable(randomVar);
            omegas.set(column, paramHelper.getOmegaFromRandomVarName(secondVariable));
        }
    }

    /**
     * This method will reverse the map and return a tree map (ordered in natural order of keys).
     * 
     * @param map
     * @return
     */
    private <K,V> TreeMap<V,K> reverseMap(Map<K,V> map) {
        TreeMap<V,K> rev = new TreeMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet()){
            if(!rev.containsKey(entry.getValue())){
                rev.put(entry.getValue(), entry.getKey());    
            }
        }
        return rev;
    }

    /**
     * Initialise lower half of the mirrored matrix by empty omega variables.
     * 
     * @param row
     * @param omegas
     */
    private void initialiseRowElements(int row, List<OmegaStatement> omegas) {
        for(int i=0; i <=row ; i++){
            if(omegas.get(i)==null){
                omegas.set(i, createOmegaWithEmptyScalar(EMPTY_VARIABLE));
            }
        }
    }
    /**
     * We set omega blocks matrix using omegas with empty scalar (i.e. value '0') so that 
     * if there is no coefficient value set for a random value pair in matrix, it will use this default value. 
     *  
     * @param symbol
     * @return
     */
    private OmegaStatement createOmegaWithEmptyScalar(String symbol) {
        OmegaStatement omega;
        omega = new OmegaStatement(symbol);
        ScalarRhs scalar = createScalarRhs(symbol, createScalar(0));  
        omega.setInitialEstimate(scalar);
        return omega;
    }

    /**
     * This method will create scalar Rhs object for a symbol from the scalar value provided.
     *  
     * @param symbol
     * @param scalar
     * @return ScalarRhs object
     */
    private ScalarRhs createScalarRhs(String symbol,JAXBElement<?> scalar) {
        ScalarRhs scalarRhs = new ScalarRhs();
        scalarRhs.setScalar(scalar);
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);
        scalarRhs.setSymbRef(symbRef);
        return scalarRhs;
    }

    /**
     * Creates scalar for the value provided.
     * This method will be helpful when only value or no value is provided in case of correlation coefficients.
     *  
     * @param value
     * @return
     */
    private static JAXBElement<IntValue> createScalar(Integer value) {
        IntValue intValue = new IntValue();
        intValue.setValue(BigInteger.valueOf(value));
        ObjectFactory factory = new ObjectFactory();
        return factory.createInt(intValue);
    }

    /**
     * This method will get existing omega for coefficient 
     * and it will create new one if there no omega ( or coeffiecient specified is only value) 
     * @param correlation
     * @return
     */
    private OmegaStatement getOmegaForCoefficient(ScalarRhs coeff, ParameterRandomVariable firstVariable, ParameterRandomVariable secondVariable) {
        Preconditions.checkNotNull(coeff, "coefficient value should not be null");
        String firstVar = RandomVariableHelper.getNameFromParamRandomVariable(firstVariable);
        String secondVar = RandomVariableHelper.getNameFromParamRandomVariable(secondVariable);

        if(coeff.getSymbRef()!=null){
            return paramHelper.getOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef()); 
        }else if(coeff.getScalar()!=null || coeff.getEquation()!=null){
            OmegaStatement omega = new OmegaStatement(firstVar+"_"+secondVar);
            omega.setInitialEstimate(coeff);
            return omega;
        }else {
            throw new IllegalArgumentException("The Scalarrhs coefficient related to variables "+firstVar+" and "+secondVar
                +" should have either variable, scalar value or equation specified."); 
        }
    }

    /**
     * Creates title for omega blocks.
     * Currently it gets block count for BLOCK(n) and identify in correlations are used.
     * This will be updated for matrix types in future.
     * 
     * @param correlations
     * @param etaToOmagas
     * @return
     */
    private String createOmegaBlockTitle(List<CorrelationRef> correlations, Map<String, Integer> etaToOmagas) {
        Integer blocksCount = etaToOmagas.size();
        StringBuilder description = new StringBuilder();
        //This will change in case of 0.4 as it will need to deal with matrix types as well.
        description.append((isCorrelation(correlations))?NmConstant.CORRELATION:" ");
        description.append((isOmegaBlockFromStdDev)?" "+NmConstant.SD:"");
        String title = String.format(Formatter.endline()+"%s %s", Formatter.omegaBlock(blocksCount),description);
        return title;
    }

    /**
     * Checks if correlation exists and returns the result.
     * 
     * @param correlations
     * @return
     */
    private Boolean isCorrelation(List<CorrelationRef> correlations) {
        if(correlations!=null && !correlations.isEmpty()){
            for(CorrelationRef correlation : correlations){
                if(correlation.isCorrelation()){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method will return index from ordered eta map for random var name provided. 
     * 
     * @param randomVariable
     * @param etaToOmagas
     * @return
     */
    private int getOrderedEtaIndex(String randomVariable, Map<String, Integer> etaToOmagas) {
        return (etaToOmagas.get(randomVariable)>0)?etaToOmagas.get(randomVariable)-1:0;
    }

    /**
     * Collects correlations from all the prameter blocks. 
     * 
     * @return
     */
    private void getAllCorrelations() {
        //        List<CorrelationRef> correlations = new ArrayList<CorrelationRef>();
        List<ParameterBlock> parameterBlocks = paramHelper.getScriptDefinition().getParameterBlocks();
        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                Iterator<CorrelationRef> itr = block.getCorrelations().iterator();
                while(itr.hasNext()){
                    CorrelationRef correlation = itr.next();
                    if(etasInIOV.contains(correlation.rnd1.getSymbId()) 
                            || etasInIOV.contains(correlation.rnd1.getSymbId())){
                        iovCorrelations.add(correlation);
                    }else{
                        nonIovCorrelations.add(correlation);
                    }
                }
            }
        }
    }

    /**
     * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
     * 
     * @param correlations
     * @param etas
     * @param omegaBlocks
     */
    private void initialiseOmegaBlocks(List<CorrelationRef> correlations, 
            Map<String, List<OmegaStatement>> omegaBlocks, Map<String, Integer> etaToOmagas, Set<String> etas){
        omegaBlocks.clear();

        for(CorrelationRef correlation : correlations){
            //Need to set SD attribute for whole block if even a single value is from std dev
            setStdDevAttributeForOmegaBlock(correlation);
        }

        for(Iterator<Entry<String, Integer>> it = etaToOmagas.entrySet().iterator(); it.hasNext();) {
            if(!etas.contains(it.next().getKey())){
                it.remove();
            }
        }

        for(String eta : etaToOmagas.keySet()){
            ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
            for(int i=0;i<etaToOmagas.get(eta);i++) statements.add(null);
            omegaBlocks.put(eta, statements);
        }
    }

    private void setStdDevAttributeForOmegaBlock(CorrelationRef correlation) {
        if(!isOmegaBlockFromStdDev){
            isOmegaBlockFromStdDev = RandomVariableHelper.isParamFromStdDev(correlation.rnd1) || RandomVariableHelper.isParamFromStdDev(correlation.rnd1);
        }
    }

    public void setEtaToOmagasInIOV(Map<String, Integer> etaToOmagaMap) {
        this.etaToOmagasInIOV = new HashMap<String, Integer>(etaToOmagaMap);
    }

    public void setEtaToOmagasInNonIOV(Map<String, Integer> etaToOmagaMap) {
        this.etaToOmagasInNonIOV = new HashMap<String, Integer>(etaToOmagaMap);
    }

    public Map<Integer, String> getOmegaOrderToEtasInIOV() {
        return omegaOrderToEtasInIOV;
    }

    public Map<Integer, String> getOmegaOrderToEtasInNonIOV() {
        return omegaOrderToEtasInNonIOV;
    }

    public String getOmegaBlockTitleInIOV() {
        return omegaBlockTitleInIOV;
    }

    public String getOmegaBlockTitleInNonIOV() {
        return omegaBlockTitleInNonIOV;
    }

    public List<CorrelationRef> getIovCorrelations() {
        return iovCorrelations;
    }

    public List<CorrelationRef> getNonIovCorrelations() {
        return nonIovCorrelations;
    }

    public Map<String, List<OmegaStatement>> getOmegaBlocksInNonIOV() {
        return omegaBlocksInNonIOV;
    }

    public Map<String, List<OmegaStatement>> getOmegaBlocksInIOV() {
        return omegaBlocksInIOV;
    }


    public void setOmegaBlockTitleInIOV(String omegaBlockTitleInIOV) {
        this.omegaBlockTitleInIOV = omegaBlockTitleInIOV;
    }


    public void setOmegaBlockTitleInNonIOV(String omegaBlockTitleInNonIOV) {
        this.omegaBlockTitleInNonIOV = omegaBlockTitleInNonIOV;
    }
}
