/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.ObjectFactory;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

public class OmegaBlockStatement {

    private final Map<String, List<OmegaStatement>> omegaBlocks = new HashMap<String, List<OmegaStatement>>();
    private Map<String, String> etasToOmegasInCorrelation = new LinkedHashMap<String, String>();

    private String omegaBlockTitle;
    private Boolean isOmegaBlockFromStdDev = false;
    private Map<String, Integer> etaToOmagaMap = new HashMap<String, Integer>();
    private Map<Integer, String> orderedEtasToOmegaMap = new TreeMap<Integer, String>();
    private ParametersHelper paramHelper;

    public OmegaBlockStatement(ParametersHelper parameters) {
        paramHelper = parameters;
    }


    /**
     * This method will create omega blocks if there are any.
     * We will need ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     *  
     * @return
     */
    public void createOmegaBlocks(){
        List<CorrelationRef> correlations = getAllCorrelations();

        if(!correlations.isEmpty()){
            initialiseOmegaBlocks(correlations);
            orderedEtasToOmegaMap = reverseMap(etaToOmagaMap);
            omegaBlockTitle = createOmegaBlockTitle(correlations);

            for(String eta : orderedEtasToOmegaMap.values()){
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
                        String symbId = paramHelper.getNameFromParamRandomVariable(secondRandomVar);
                        omegas.set(row, paramHelper.getOmegaFromRandomVarName(symbId));
                    }

                    //add coefficient associated with random var1 and random var2 at [i,j] 
                    //which is mirrored with [j,i] ([j,i] will be empty as its not required) 
                    if(omegas.get(column)==null){
                        OmegaStatement omega = getOmegaForCoefficient(correlation.correlationCoefficient, firstRandomVar, secondRandomVar);
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
     * Initialise lower half of the mirrored matrix by empty omega variables.
     * 
     * @param row
     * @param omegas
     */
    private void initialiseRowElements(int row, List<OmegaStatement> omegas) {
        for(int i=0; i <=row ; i++){
            if(omegas.get(i)==null){
                omegas.set(i, createOmegaWithEmptyScalar("Empty Variable"));
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
        ScalarRhs scalar = ConversionContext.createScalarRhs(symbol, createScalar(0));  
        omega.setInitialEstimate(scalar);
        return omega;
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
     * 
     * @param correlation
     * @return
     */
    private OmegaStatement getOmegaForCoefficient(ScalarRhs coeff, ParameterRandomVariable firstVar, ParameterRandomVariable secondVar) {
        OmegaStatement omega = null;
        if(coeff.getSymbRef()!=null){
            omega = paramHelper.getOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef());
        }else if(omega == null && coeff.getScalar()!=null){
            omega = new OmegaStatement(firstVar.getSymbId()+"_"+secondVar.getSymbId());
            omega.setInitialEstimate(coeff);
        }
        return omega;
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
        Integer blocksCount = etaToOmagaMap.size();
        StringBuilder description = new StringBuilder();
        //This will change in case of 0.4 as it will need to deal with matrix types as well.
        description.append((!correlations.isEmpty())?NmConstant.CORRELATION:" ");
        description.append((isOmegaBlockFromStdDev)?" "+NmConstant.SD:"");
        String title = String.format(Formatter.endline()+"%s %s", Formatter.omegaBlock(blocksCount),description);
        return title;
    }

    /**
     * This method will return index from ordered eta map for random var name provided. 
     * 
     * @param randomVariable
     * @return
     */
    private int getOrderedEtaIndex(String randomVariable) {
        return (etaToOmagaMap.get(randomVariable)>0)?etaToOmagaMap.get(randomVariable)-1:0;
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
        if(etaToOmagaMap.get(randomVar1.getSymbId())== 1 && eta.equals(randomVar1.getSymbId())){
            List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
            String symbId = paramHelper.getNameFromParamRandomVariable(randomVar1);
            matrixRow.add(paramHelper.getOmegaFromRandomVarName(symbId));
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
            paramHelper.addCorrelationToMap(etasToOmegasInCorrelation,correlation);
        }

        for(Iterator<Entry<String, Integer>> it = etaToOmagaMap.entrySet().iterator(); it.hasNext();) {
            if(!etasToOmegasInCorrelation.keySet().contains(it.next().getKey())){
                it.remove();
            }
        }

        for(String eta : etaToOmagaMap.keySet()){
            ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
            for(int i=0;i<etaToOmagaMap.keySet().size();i++) statements.add(null);
            omegaBlocks.put(eta, statements);
        }
    }

    /**
     * This method will reverse the map and return a tree map (ordered in natural order of keys).
     * 
     * @param map
     * @return
     */
    private<K,V> TreeMap<V,K> reverseMap(Map<K,V> map) {
        TreeMap<V,K> rev = new TreeMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            rev.put(entry.getValue(), entry.getKey());
        return rev;
    }

    private void setStdDevAttributeForOmegaBlock(CorrelationRef correlation) {
        if(!isOmegaBlockFromStdDev){
            isOmegaBlockFromStdDev = paramHelper.isParamFromStdDev(correlation.rnd1) || paramHelper.isParamFromStdDev(correlation.rnd1);
        }
    }

    public Map<String, Integer> getEtaToOmagaMap() {
        return etaToOmagaMap;
    }

    public void setEtaToOmagaMap(Map<String, Integer> etaToOmagaMap) {
        this.etaToOmagaMap = etaToOmagaMap;
    }

    public Map<String, List<OmegaStatement>> getOmegaBlocks() {
        return omegaBlocks;
    }

    public Map<String, String> getEtasToOmegasInCorrelation() {
        return etasToOmegasInCorrelation;
    }
    public Map<Integer, String> getOrderedEtasToOmegaMap() {
        return orderedEtasToOmegaMap;
    }


    public String getOmegaBlockTitle() {
        return omegaBlockTitle;
    }

}
