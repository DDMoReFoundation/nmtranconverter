/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.GeneralCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.LinearCovariate;

/**
 * This class handles ordered thetas and associated methods to retrieve information from it.
 *  
 */
public class OrderedThetasHandler {
    private static final String MU = "MU_";
    private final TreeMap<Integer, String> orderedThetas = new TreeMap<Integer, String>();
    private final ScriptDefinition scriptDefinition;

    public OrderedThetasHandler(ScriptDefinition scriptDefinition){
        Preconditions.checkNotNull(scriptDefinition, "Script definition Context cannot be null");
        this.scriptDefinition = scriptDefinition;
    }

    /**
     * Create ordered thetas to eta map from ordered etas map.
     * The order is used to add Thetas in order of thetas.
     */
    public void createOrderedThetasToEta(Map<String, Integer> orderedEtas){        
        for(Integer nextEtaOrder : orderedEtas.values()){
            if(StringUtils.isEmpty(orderedThetas.get(nextEtaOrder))){
                addToThetasOrderMap(orderedEtas, nextEtaOrder);
            }
        }
    }

    /**
     * Creates ordered thetas list with help of etasOrderMap and individual parameters
     * @param orderedEtas
     * @param nextEtaOrder
     */
    private void addToThetasOrderMap(Map<String, Integer> orderedEtas, Integer nextEtaOrder) {
        for(ParameterBlock block : scriptDefinition.getParameterBlocks()){
            for(IndividualParameter parameterType: block.getIndividualParameters()){
                final GaussianModel gaussianModel = parameterType.getGaussianModel();
                if (gaussianModel != null) {
                    String popSymbol = getPopSymbol(gaussianModel);
                    List<ParameterRandomEffect> randomEffects = gaussianModel.getRandomEffects();
                    for (ParameterRandomEffect randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        if(orderedEtas.get(eta).equals(nextEtaOrder)){
                            orderedThetas.put(nextEtaOrder, popSymbol);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * This method gets population parameter symbol id from linear covariate of a gaussian model.
     * 
     * @param gaussianModel
     * @return population parameter symbol
     */
    public String getPopSymbol(final GaussianModel gaussianModel) {
        Preconditions.checkNotNull(gaussianModel, " Gaussian model cannot be null");
        LinearCovariate lcov =  gaussianModel.getLinearCovariate();
        if(lcov!=null && lcov.getPopulationParameter()!=null){
            return getSymbIdFromRhs(lcov.getPopulationParameter().getAssign());
        }else if(gaussianModel.getGeneralCovariate()!=null){
            GeneralCovariate generalCov = gaussianModel.getGeneralCovariate();
            return getSymbIdFromRhs(generalCov.getAssign());
        }else{
            throw new IllegalArgumentException("Pop symbol missing.The population parameter is not well formed.");
        }
    }

    /**
     * Retrieves symbol from the symbId associated with Rhs. 
     * If Rhs doesnt have symbId then looks for it in equation.
     * 
     * @param assign
     * @return variable symbol
     */
    private String getSymbIdFromRhs(Rhs assign){
        Preconditions.checkNotNull(assign);
        Equation eq = assign.getEquation();
        if(assign.getSymbRef()!=null){
            return assign.getSymbRef().getSymbIdRef();
        }else if(eq!=null && eq.getSymbRef()!=null){
            return eq.getSymbRef().getSymbIdRef();
        }else {
            throw new IllegalArgumentException("Variable symbol missing in assignment. The population parameter is not well formed.");
        }
    }

    /**
     * Get MU symbol for population parameter symbol provided
     * @param popSymbol
     * @return respective MU symbol
     */
    public String getMUSymbol(String popSymbol){
        Preconditions.checkArgument(!orderedThetas.isEmpty(), "Ordered thetas do not exist or not arranged yet.");
        for(Integer thetaOrder: orderedThetas.keySet()){
            if(popSymbol.equals(orderedThetas.get(thetaOrder))){
                return new String(MU+thetaOrder);
            }
        }
        return new String(); 
    }

    public Map<Integer, String> getOrderedThetas() {
        return orderedThetas;
    }

}
