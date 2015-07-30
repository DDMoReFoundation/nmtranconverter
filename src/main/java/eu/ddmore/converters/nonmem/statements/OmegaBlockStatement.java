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
    private final Map<String, List<OmegaStatement>> omegaBlocks = new HashMap<String, List<OmegaStatement>>();

    private String omegaBlockTitle;
    private Boolean isOmegaBlockFromStdDev = false;
    private Map<String, Integer> etaToOmagas = new HashMap<String, Integer>();
    private Map<Integer, String> omegaOrderToEtas = new TreeMap<Integer, String>();
    private final ParametersHelper paramHelper;
    private final Set<String> etasInCorrelation;

    public OmegaBlockStatement(ParametersHelper parameter, OrderedEtasHandler orderedEtasHandler) {
        Preconditions.checkNotNull(parameter, "parameter should not be null");
        Preconditions.checkNotNull(orderedEtasHandler, "ordered etas handler should not be null");
        this.paramHelper = parameter;
        etasInCorrelation = orderedEtasHandler.getEtasToOmegasInCorrelation().keySet();
        setEtaToOmagas(orderedEtasHandler.getOrderedEtas());
    }


    /**
     * This method will create omega blocks if there are any.
     * We will need ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     *  
     */
    public void createOmegaBlocks(){
        List<CorrelationRef> correlations = getAllCorrelations();

        if(!correlations.isEmpty()){
            initialiseOmegaBlocks(correlations);
            omegaOrderToEtas = reverseMap(etaToOmagas);
            omegaBlockTitle = createOmegaBlockTitle(correlations);

            for(String eta : omegaOrderToEtas.values()){
                Iterator<CorrelationRef> iterator = correlations.iterator();

                while(iterator.hasNext()){
                    CorrelationRef correlation = iterator.next();
                    ParameterRandomVariable firstRandomVar = correlation.rnd1;
                    ParameterRandomVariable secondRandomVar = correlation.rnd2;

                    // row = i column = j
                    int column = getOrderedEtaIndex(firstRandomVar.getSymbId());
                    int row = getOrderedEtaIndex(secondRandomVar.getSymbId());

                    if(column > row){
                        firstRandomVar = correlation.rnd2;
                        secondRandomVar = correlation.rnd1;
                        int swap = column;
                        column = row;
                        row = swap;
                    }

                    createFirstMatrixRow(eta, firstRandomVar);
                    List<OmegaStatement> omegas = omegaBlocks.get(secondRandomVar.getSymbId());
                    // add random var to matrix at [i,i]
                    if(omegas.get(row)==null){
                        initialiseRowElements(row, omegas);
                        String secondVariable = RandomVariableHelper.getNameFromParamRandomVariable(secondRandomVar);
                        omegas.set(row, paramHelper.getOmegaFromRandomVarName(secondVariable));
                    }

                    //add coefficient associated with random var1 and random var2 at [i,j] 
                    //which is mirrored with [j,i] ([j,i] will be empty as its not required) 
                    if(omegas.get(column)==null || omegas.get(column).getSymbId().equals(EMPTY_VARIABLE)){
                        OmegaStatement omega = null ;
                        if(correlation.isCorrelation()){
                            omega = getOmegaForCoefficient(correlation.correlationCoefficient, firstRandomVar, secondRandomVar);
                        }else if(correlation.isCovariance()){
                            omega = getOmegaForCoefficient(correlation.covariance, firstRandomVar, secondRandomVar);
                        }
                        if(omega != null){
                            omegas.set(column,omega);
                        }
                    }
                    iterator.remove();
                }
            }
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
    private OmegaStatement getOmegaForCoefficient(ScalarRhs coeff, ParameterRandomVariable firstVar, ParameterRandomVariable secondVar) {
        Preconditions.checkNotNull(coeff, "coefficient value should not be null");
        if(coeff.getSymbRef()!=null){
            return paramHelper.getOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef()); 
        }else if(coeff.getScalar()!=null || coeff.getEquation()!=null){
            String firstVariable = RandomVariableHelper.getNameFromParamRandomVariable(firstVar);
            String secondVariable = RandomVariableHelper.getNameFromParamRandomVariable(secondVar);
            OmegaStatement omega = new OmegaStatement(firstVariable+"_"+secondVariable);
            omega.setInitialEstimate(coeff);
            return omega;
        }else {
            throw new IllegalArgumentException("The Scalarrhs coefficient should have either variable or scalar value specified. "); 
        }
    }

    /**
     * Creates title for omega blocks.
     * Currently it gets block count for BLOCK(n) and identify in correlations are used.
     * This will be updated for matrix types in future.
     * 
     * @param correlations
     * @return
     */
    private String createOmegaBlockTitle(List<CorrelationRef> correlations) {
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
     * @return
     */
    private int getOrderedEtaIndex(String randomVariable) {
        return (etaToOmagas.get(randomVariable)>0)?etaToOmagas.get(randomVariable)-1:0;
    }

    /**
     * Collects correlations from all the prameter blocks. 
     * 
     * @return
     */
    private List<CorrelationRef> getAllCorrelations() {
        List<CorrelationRef> correlations = new ArrayList<CorrelationRef>();
        List<ParameterBlock> parameterBlocks = paramHelper.getScriptDefinition().getParameterBlocks();
        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                correlations.addAll(block.getCorrelations());				
            }
        }
        return correlations;
    }

    /**
     * Creates first matrix row which will have only first omega as element.
     * 
     * @param eta
     * @param randomVar1
     */
    private void createFirstMatrixRow(String eta, ParameterRandomVariable randomVar1) {
        if(etaToOmagas.get(randomVar1.getSymbId())== 1 && eta.equals(randomVar1.getSymbId())){
            List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
            String firstVariable = RandomVariableHelper.getNameFromParamRandomVariable(randomVar1);
            matrixRow.add(paramHelper.getOmegaFromRandomVarName(firstVariable));
            omegaBlocks.put(randomVar1.getSymbId(), matrixRow);
        }
    }

    /**
     * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
     * 
     * @param correlations
     */
    private void initialiseOmegaBlocks(List<CorrelationRef> correlations){
        omegaBlocks.clear();

        for(CorrelationRef correlation : correlations){
            //Need to set SD attribute for whole block if even a single value is from std dev
            setStdDevAttributeForOmegaBlock(correlation);
        }

        for(Iterator<Entry<String, Integer>> it = etaToOmagas.entrySet().iterator(); it.hasNext();) {
            if(!etasInCorrelation.contains(it.next().getKey())){
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

    public Map<String, Integer> getEtaToOmagas() {
        return etaToOmagas;
    }

    public void setEtaToOmagas(Map<String, Integer> etaToOmagaMap) {
        this.etaToOmagas = new HashMap<String, Integer>(etaToOmagaMap);
    }

    public Map<String, List<OmegaStatement>> getOmegaBlocks() {
        return omegaBlocks;
    }

    public Map<Integer, String> getOmegaOrderToEtas() {
        return omegaOrderToEtas;
    }

    public String getOmegaBlockTitle() {
        return omegaBlockTitle;
    }
}
