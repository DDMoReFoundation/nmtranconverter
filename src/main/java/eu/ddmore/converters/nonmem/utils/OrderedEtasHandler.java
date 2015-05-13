package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class deals with ordered etas with help of correlations specified. 
 * Also it arranges eta to correlations mapping as well.
 */
public class OrderedEtasHandler {

    private final List<String> orderedEtas = new ArrayList<String>();
    ScriptDefinition scriptDefinition;

    public OrderedEtasHandler(ScriptDefinition scriptDefinition) {
        this.scriptDefinition = scriptDefinition;
        initialiseOrderedEtas(scriptDefinition);
    }

    /**
     * Checks the order of Etas and use this order while arranging Omega blocks.
     * This method will create map for EtaOrder which will be order for respective Omegas as well.
     * @return 
     */
    public Map<String, Integer> createOrderedEtasMap(){
        LinkedHashMap<String, Integer> etasOrderMap = new LinkedHashMap<String, Integer>();
        //We need to have this as list as this will retains order of etas
        OrderedEtasHandler etasHandler = new OrderedEtasHandler(scriptDefinition);
        List<String> etasOrder = etasHandler.getOrderedEtas();

        if(!etasOrder.isEmpty()){
            Integer etaCount = 0;
            Map<String, String> etaTocorrelationsMap = addCorrelationValuesToMap(getAllCorrelations());

            List<String> nonOmegaBlockEtas = new ArrayList<String>();
            //order etas map
            for(String eta : etasOrder) {
                //no correlations so no Omega block
                if(etaTocorrelationsMap.keySet().contains(eta)){
                    ++etaCount;
                    etasOrderMap.put(eta,etaCount);
                }else{
                    nonOmegaBlockEtas.add(eta);
                }
            }
            for(String nonOmegaEta : nonOmegaBlockEtas){
                etasOrderMap.put(nonOmegaEta, ++etaCount);
            }
        }

        return etasOrderMap;
    }
    
    /**
     * Adds correlations reference to map provided for eta to correlations map.
     * 
     * @param etaToCorrelations
     * @param correlation
     */
    public void addCorrelationToMap(Map<String, String> etaToCorrelations, CorrelationRef correlation) {
        String firstVar = correlation.rnd1.getSymbId();
        String secondVar = correlation.rnd2.getSymbId();
        String coefficient = "";
        if(correlation.correlationCoefficient.getSymbRef()!=null)
            coefficient = correlation.correlationCoefficient.getSymbRef().getSymbIdRef();
        //add to correlations map
        etaToCorrelations.put(firstVar,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd1));
        etaToCorrelations.put(secondVar,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd2));
        etaToCorrelations.put(coefficient,coefficient);
    }

    /**
     * Creates ordered Etas map which gets all etas list from individual parameters and parameter block. 
     * @return
     */
    private List<String> initialiseOrderedEtas(ScriptDefinition scriptDefinition) {
        List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
        for(ParameterBlock block : blocks ){
            for(IndividualParameter parameterType: block.getIndividualParameters()){
                if (parameterType.getGaussianModel() != null) {
                    List<ParameterRandomEffect> randomEffects = parameterType.getGaussianModel().getRandomEffects();
                    for (ParameterRandomEffect randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        if(!orderedEtas.contains(eta)){
                            orderedEtas.add(eta);
                        }
                    }
                }
            }
            Collection<ParameterRandomVariable> epsilons =  ConversionContext.getEpsilonRandomVariables(scriptDefinition);
            for(ParameterRandomVariable variable : block.getRandomVariables()){
                if(!epsilons.contains(variable)){
                    String eta = variable.getSymbId();
                    if(!orderedEtas.contains(eta)){
                        orderedEtas.add(eta);
                    }
                }
            }
        }
        return orderedEtas;
    }

    /**
     * Adds correlation values to eta to correlations map
     * @param correlations
     * @return
     */
    private Map<String, String> addCorrelationValuesToMap(List<CorrelationRef> correlations) {
        //We need to have it as linked hash map so that order in which correlations are added to map will be retained.
        LinkedHashMap<String, String> etaTocorrelations = new LinkedHashMap<String, String>();
        for(CorrelationRef correlation : correlations){
            addCorrelationToMap(etaTocorrelations,correlation);  
        }
        return etaTocorrelations;
    }

    /**
     * Collects correlations from all the parameter blocks. 
     * 
     * @return
     */
    private List<CorrelationRef> getAllCorrelations() {
        List<CorrelationRef> correlations = new ArrayList<CorrelationRef>();
        List<ParameterBlock> parameterBlocks = scriptDefinition.getParameterBlocks();
        if(!parameterBlocks.isEmpty()){
            for(ParameterBlock block : parameterBlocks){
                correlations.addAll(block.getCorrelations());               
            }
        }
        return correlations;
    }

    public List<String> getOrderedEtas() {
        return orderedEtas;
    }

}