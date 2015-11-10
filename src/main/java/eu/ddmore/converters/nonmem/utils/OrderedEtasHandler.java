/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VarLevel;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class deals with ordered etas with help of correlations specified. 
 * Also it arranges eta to correlations mapping as well.
 */
public class OrderedEtasHandler {

    private final Set<Eta> allEtas = new LinkedHashSet<Eta>();
    private final Set<Eta> allOrderedEtas = new TreeSet<Eta>();

    private final Set<Eta> orderedEtasInNonIOV = new TreeSet<Eta>();
    private final Set<Eta> orderedEtasInIOV = new TreeSet<Eta>();

    private final List<CorrelationRef> iovCorrelations = new ArrayList<CorrelationRef>();
    private final List<CorrelationRef> nonIovCorrelations = new ArrayList<CorrelationRef>();

    //We need to have it as linked hash map so that order in which correlations are added to map will be retained.
    private Map<Eta, String> etasToOmegasInCorrelation = new LinkedHashMap<Eta, String>();
    private Map<Eta, String> etasToOmegasInIOV = new LinkedHashMap<Eta, String>();
    private final ScriptDefinition scriptDefinition;
    private final ConversionContext context;

    public OrderedEtasHandler(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null");
        this.context = context;
        Preconditions.checkNotNull(context.getScriptDefinition(), "Script definition cannot be null");
        this.scriptDefinition = context.getScriptDefinition();
        initialise();
    }

    private void initialise(){
        arrangeAllCorrelations();
        addcorrelationEtas(getIovCorrelations(), etasToOmegasInIOV, VarLevel.IOV);
        addcorrelationEtas(getNonIovCorrelations(), etasToOmegasInCorrelation, VarLevel.IIV);
        retrieveAllEtas(scriptDefinition);
        createAllOrderedEtasMap();
    }

    private void addcorrelationEtas(List<CorrelationRef>  correlations, Map<Eta, String> etaToOmegas, VarLevel varLevel){
        for(CorrelationRef correlation : correlations){
            addCorrelationEtas(etaToOmegas, varLevel, correlation);
        }        
    }

    private Set<Eta> createAllOrderedEtasMap(){

        if(!allEtas.isEmpty()){
            int etaCount = 0;

            int corrEtaCount = 0;
            for(Eta eta : allEtas) {

                //no correlations so no Omega block
                if(etasToOmegasInCorrelation.keySet().contains(eta)){
                    for(Eta nonIovEta : etasToOmegasInCorrelation.keySet()){
                        if(eta.getEtaSymbol().equals(nonIovEta.getEtaSymbol()) ){
                            nonIovEta.setOrder(++etaCount);
                            nonIovEta.setOrderInCorr(++corrEtaCount);
                            nonIovEta.setVarLevel(VarLevel.IIV);
                            allOrderedEtas.add(nonIovEta);
                            orderedEtasInNonIOV.add(nonIovEta);
                        }
                    }
                }else if(!etasToOmegasInIOV.keySet().contains(eta)){
                    eta.setOrder(++etaCount);
                    eta.setVarLevel(VarLevel.IIV);
                    allOrderedEtas.add(eta);
                }
            }

            int iovEtaCount = 0;
            for(Eta iovEta : etasToOmegasInIOV.keySet()){
                if(iovEta.getVarLevel().equals(VarLevel.IOV)){
                    iovEta.setOrder(++etaCount);
                    iovEta.setOrderInCorr(++iovEtaCount);
                    iovEta.setVarLevel(VarLevel.IOV);
                    String etaSymbolForIOV = context.getIovHandler().getColumnWithOcc().getColumnId()+"_"+iovEta.getOmegaName();
                    iovEta.setEtaSymbolForIOV(etaSymbolForIOV);
                    allOrderedEtas.add(iovEta);
                    orderedEtasInIOV.add(iovEta);
                }
            }
        }
        return allOrderedEtas;
    }

    /**
     * Adds correlations reference to map provided for eta to correlations map.
     * 
     * @param etaToCorrelations
     * @param correlation
     */
    private void addCorrelationEtas(Map<Eta, String> etaToCorrelations, VarLevel level, CorrelationRef correlation) {
        Preconditions.checkNotNull(correlation, "Correlation reference cannot be null");

        String firstVar = correlation.rnd1.getSymbId();
        String secondVar = correlation.rnd2.getSymbId();
        String coefficient = "";
        if(correlation.isCorrelation()){
            coefficient = getVariableOrValueFromScalarRhs(correlation.correlationCoefficient);
        }else if(correlation.isCovariance()){
            coefficient = getVariableOrValueFromScalarRhs(correlation.covariance).toString();
        }
        //add to correlations
        Eta firstEta = new Eta(firstVar);
        firstEta.setVarLevel(level);
        firstEta.setCorrelationRelated(true);
        firstEta.setOmegaName(RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd1));

        Eta secondEta = new Eta(secondVar);
        secondEta.setVarLevel(level);
        secondEta.setCorrelationRelated(true);
        secondEta.setOmegaName(RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd2));

        Eta coeffEta = new Eta(coefficient);
        coeffEta.setOmegaName(coefficient);
        coeffEta.setVarLevel(VarLevel.NONE);

        etaToCorrelations.put(firstEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd1));
        etaToCorrelations.put(secondEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd2));
        etaToCorrelations.put(coeffEta,coefficient);
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
    private Set<Eta> retrieveAllEtas(ScriptDefinition scriptDefinition) {
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

    private void addRandomVarToAllEtas(String etaSymbol) {
        Eta eta = new Eta(etaSymbol);
        if(!allEtas.contains(eta)){
            allEtas.add(eta);
        }
    }

    /**
     * Collects correlations from all the parameter blocks. 
     * 
     * @return
     */
//    private void addAllCorrelations() {
//        List<ParameterBlock> parameterBlocks = scriptDefinition.getParameterBlocks();
//        if(!parameterBlocks.isEmpty()){
//            for(ParameterBlock block : parameterBlocks){
//                correlations.addAll(block.getCorrelations());
//            }
//        }
//    }
    
    /**
     * Collects correlations from all the prameter blocks. 
     * 
     * @return
     */
    private void arrangeAllCorrelations() {
        List<ParameterBlock> parameterBlocks = scriptDefinition.getParameterBlocks();
        List<String> occRandomVars = context.getIovHandler().getOccasionRandomVariables();

        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                Iterator<CorrelationRef> itr = block.getCorrelations().iterator();
                while(itr.hasNext()){
                    CorrelationRef correlation = itr.next();
                    boolean isIOV = false;
                    if(occRandomVars.contains(correlation.rnd1.getSymbId()) 
                            || occRandomVars.contains(correlation.rnd2.getSymbId())){
                            iovCorrelations.add(correlation);
                            isIOV = true;
                        }
                    if(!isIOV){
                        nonIovCorrelations.add(correlation);
                    }
                }
            }
        }
    }

    public Set<Eta> getAllOrderedEtas() {
        return allOrderedEtas;
    }

    public Map<Eta, String> getEtasToOmegasInCorrelation() {
        return etasToOmegasInCorrelation;
    }

    public Map<Eta, String> getEtasToOmegasInIOV() {
        return etasToOmegasInIOV;
    }

//    public List<CorrelationRef> getCorrelations() {
//        return correlations;
//    }

    public Set<Eta> getOrderedEtasInIOV() {
        return orderedEtasInIOV;
    }

    public Set<Eta> getOrderedEtasInNonIOV() {
        return orderedEtasInNonIOV;
    }

    
    public List<CorrelationRef> getIovCorrelations() {
        return iovCorrelations;
    }

    
    public List<CorrelationRef> getNonIovCorrelations() {
        return nonIovCorrelations;
    }
}
