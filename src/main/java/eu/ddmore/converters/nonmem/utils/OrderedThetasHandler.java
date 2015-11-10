/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.tree.BinaryTree;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
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
    private final TreeMap<Integer, String> orderedThetas = new TreeMap<Integer, String>();
    private final ConversionContext context;
    private ScriptDefinition scriptDefinition;

    public OrderedThetasHandler(ConversionContext conversionContext){
        Preconditions.checkNotNull(conversionContext, "Conversion Context cannot be null");
        Preconditions.checkNotNull(conversionContext.getScriptDefinition(), "Script definition cannot be null");

        this.context = conversionContext;
        scriptDefinition = context.getScriptDefinition();
    }

    /**
     * Create ordered thetas to eta map from ordered etas map.
     * The order is used to add Thetas in order of thetas.
     */
    public void createOrderedThetasToEta(Set<Eta> orderedEtas){ 
        Preconditions.checkNotNull(orderedEtas, "Ordered etas cannot be null.");
        for(Eta eta : orderedEtas){
            addToThetasOrderMap(eta);
        }
    }

    /**
     * Creates ordered thetas list with help of etasOrderMap and individual parameters
     * @param orderedEtas
     * @param nextEtaOrder
     */
    private void addToThetasOrderMap(Eta eta) {
        for(ParameterBlock block : scriptDefinition.getParameterBlocks()){
            for(IndividualParameter parameterType: block.getIndividualParameters()){
                final GaussianModel gaussianModel = parameterType.getGaussianModel();
                if (gaussianModel != null) {
                    if(addPopSymbolToOrderedEtas(eta, gaussianModel)) 
                        return;
                }
            }
        }
    }

    private boolean addPopSymbolToOrderedEtas(Eta eta, GaussianModel gaussianModel) {
        String popSymbol = getPopSymbol(gaussianModel);
        List<ParameterRandomEffect> randomEffects = gaussianModel.getRandomEffects();
        for (ParameterRandomEffect randomEffect : randomEffects) {
            if (randomEffect == null) continue;
            String etaToAdd = randomEffect.getSymbRef().get(0).getSymbIdRef();
            if(eta.getEtaSymbol().equals(etaToAdd)){
                orderedThetas.put(eta.getOrder(), popSymbol);
                return true;
            }
        }
        return false;
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
        }else if(eq!=null){
            BinaryTree bt = context.getLexer().getTreeMaker().newInstance(eq);
            if(eq.getSymbRef()!=null){
                return eq.getSymbRef().getSymbIdRef();
            }else if(eq.getUniop()!=null){
                return context.parse(eq.getUniop(),bt);
            }else if(eq.getBinop()!=null){
                return context.parse(eq.getBinop(), bt);
            } 
        }else {
            throw new IllegalArgumentException("Variable symbol missing in assignment. The population parameter is not well formed.");
        }
        return null;
    }

    public Map<Integer, String> getOrderedThetas() {
        return orderedThetas;
    }

}
