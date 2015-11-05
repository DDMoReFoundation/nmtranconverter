/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class deals with ordered etas with help of correlations specified. 
 * Also it arranges eta to correlations mapping as well.
 */
public class OrderedEtasHandler {

    private final List<String> allEtas = new ArrayList<String>();
    private final Map<String, Integer> allOrderedEtas = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> orderedEtasInNonIOV = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> orderedEtasInIOV = new LinkedHashMap<String, Integer>();
    private final List<CorrelationRef> correlations = new ArrayList<CorrelationRef>();
    private final Map<String, String> etasToOmegasInCorrelation;
    private final Map<String, String> etasToOmegasInIOV;
    private final ScriptDefinition scriptDefinition;
    private final ConversionContext context;

    public OrderedEtasHandler(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null");
        this.context = context;
        Preconditions.checkNotNull(context.getScriptDefinition(), "Script definition cannot be null");
        this.scriptDefinition = context.getScriptDefinition();
        addAllCorrelations();
        etasToOmegasInIOV = addEtasFromIOV();
        etasToOmegasInCorrelation = addCorrelationValuesToMap();
        retrieveAllEtas(scriptDefinition);
        createOrderedEtasMap();
    }

    /**
     * Checks the order of Etas and use this order while arranging Omega blocks.
     * This method will create map for EtaOrder which will be order for respective Omegas as well.
     * @return 
     */
    private Map<String, Integer> createOrderedEtasMap(){

        //We need to have this as list as this will retains order of etas
        List<String> etasOrder = getAllEtas();

        if(!etasOrder.isEmpty()){
            Integer etaCount = 0;

            List<String> nonOmegaBlockEtas = new ArrayList<String>();

            //order etas map
            for(String eta : etasOrder) {
                //no correlations so no Omega block
                if(!etasToOmegasInIOV.keySet().contains(eta)){
                    if(etasToOmegasInCorrelation.keySet().contains(eta)){
                        ++etaCount;
                        allOrderedEtas.put(eta,etaCount);
                        orderedEtasInNonIOV.put(eta, etaCount);
                    }else{
                        nonOmegaBlockEtas.add(eta);
                    }
                }
            }

            for(String nonOmegaEta : nonOmegaBlockEtas){
                allOrderedEtas.put(nonOmegaEta, ++etaCount);
            }

            int iovEtaCount = 0;
            for(String iovEta : etasToOmegasInIOV.keySet()){
                if(etasOrder.contains(iovEta)){
                    allOrderedEtas.put(iovEta, ++etaCount);
                    orderedEtasInIOV.put(iovEta, ++iovEtaCount);
                }
            }
        }
        return allOrderedEtas;
    }

    private Map<String, String> addEtasFromIOV() {
        LinkedHashMap<String, String> etaToIOV = new LinkedHashMap<String, String>();
        List<String> occRandomVars = context.getIovHandler().getOccasionRandomVariables();
        
        Iterator<CorrelationRef> itr = getCorrelations().iterator();
        while(itr.hasNext()){
            CorrelationRef correlation = itr.next();
            if(occRandomVars.contains(correlation.rnd1.getSymbId()) 
                    || occRandomVars.contains(correlation.rnd2.getSymbId())){
                addCorrelationToMap(etaToIOV,correlation);
                itr.remove();
            }
        }
        return etaToIOV;
    }

    /**
     * Adds correlations reference to map provided for eta to correlations map.
     * 
     * @param etaToCorrelations
     * @param correlation
     */
    private void addCorrelationToMap(Map<String, String> etaToCorrelations, CorrelationRef correlation) {
        Preconditions.checkNotNull(etaToCorrelations, "eta to correlations map cannot be null");
        Preconditions.checkNotNull(correlation, "Correlation reference cannot be null");

        String firstVar = correlation.rnd1.getSymbId();
        String secondVar = correlation.rnd2.getSymbId();
        String coefficient = "";
        if(correlation.isCorrelation()){
            coefficient = getVariableOrValueFromScalarRhs(correlation.correlationCoefficient);
        }else if(correlation.isCovariance()){
            coefficient = getVariableOrValueFromScalarRhs(correlation.covariance).toString();
        }
        //add to correlations map
        etaToCorrelations.put(firstVar,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd1));
        etaToCorrelations.put(secondVar,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd2));
        etaToCorrelations.put(coefficient,coefficient);
    }

    /**
     * Gets variable from scalar rhs if it exists or else looks for scalar value and returns in string form.
     *    
     * @param rhs
     * @return scalar variable or value
     */
    private String getVariableOrValueFromScalarRhs(ScalarRhs rhs) {
        String coefficient;
        if(rhs.getSymbRef()!=null){
            coefficient = rhs.getSymbRef().getSymbIdRef();
        }
        else{
            coefficient = ScalarValueHandler.getValueFromScalarRhs(rhs).toString();
        }
        return coefficient;
    }

    /**
     * Creates ordered Etas map which gets all etas list from individual parameters and parameter block. 
     * @return
     */
    private List<String> retrieveAllEtas(ScriptDefinition scriptDefinition) {
        List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
        for(ParameterBlock block : blocks ){
            for(IndividualParameter parameterType: block.getIndividualParameters()){
                if (parameterType.getGaussianModel() != null) {
                    List<ParameterRandomEffect> randomEffects = parameterType.getGaussianModel().getRandomEffects();
                    for (ParameterRandomEffect randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        addRandomVarToAllEtas(eta);
                    }
                }
            }

            for(ParameterRandomVariable variable : block.getLinkedRandomVariables()){
                addRandomVarToAllEtas(variable.getSymbId());
            }

            Collection<ParameterRandomVariable> epsilons =  ScriptDefinitionAccessor.getEpsilonRandomVariables(scriptDefinition);
            for(ParameterRandomVariable variable : block.getRandomVariables()){
                if(!epsilons.contains(variable)){
                    addRandomVarToAllEtas(variable.getSymbId());
                }
            }
        }
        return allEtas;
    }

    private void addRandomVarToAllEtas(String eta) {
        if(!allEtas.contains(eta)){
            allEtas.add(eta);
        }
    }

    /**
     * Adds correlation values to eta to correlations map
     * @param correlations
     * @return
     */
    private Map<String, String> addCorrelationValuesToMap() {
        //We need to have it as linked hash map so that order in which correlations are added to map will be retained.
        LinkedHashMap<String, String> etaTocorrelations = new LinkedHashMap<String, String>();
        for(CorrelationRef correlation : getCorrelations()){
            addCorrelationToMap(etaTocorrelations,correlation);
        }
        return etaTocorrelations;
    }

    /**
     * Collects correlations from all the parameter blocks. 
     * 
     * @return
     */
    private void addAllCorrelations() {
        List<ParameterBlock> parameterBlocks = scriptDefinition.getParameterBlocks();
        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                correlations.addAll(block.getCorrelations());
            }
        }
    }

    public List<String> getAllEtas() {
        return allEtas;
    }

    public Map<String, Integer> getAllOrderedEtas() {
        return allOrderedEtas;
    }

    public Map<String, String> getEtasToOmegasInCorrelation() {
        return etasToOmegasInCorrelation;
    }

    public Map<String, String> getEtasToOmegasInIOV() {
        return etasToOmegasInIOV;
    }

    public List<CorrelationRef> getCorrelations() {
        return correlations;
    }

    
    public Map<String, Integer> getOrderedEtasInIOV() {
        return orderedEtasInIOV;
    }

    
    public Map<String, Integer> getOrderedEtasInNonIOV() {
        return orderedEtasInNonIOV;
    }

}
