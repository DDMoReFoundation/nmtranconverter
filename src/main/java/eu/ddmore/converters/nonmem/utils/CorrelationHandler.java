/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VarLevel;
import eu.ddmore.converters.nonmem.statements.OmegaBlock;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;

/**
 * This class deals with ordered etas with help of correlations specified. 
 * Also it arranges eta to correlations mapping as well.
 */
public class CorrelationHandler {

    //Either we need to move etas from here or rename class as it does correlation omega and IOV omega initialisation
    private final Set<Eta> allOrderedEtas = new TreeSet<Eta>();

    private final List<CorrelationRef> iovCorrelations = new ArrayList<CorrelationRef>();
    private final List<CorrelationRef> nonIovCorrelations = new ArrayList<CorrelationRef>();
    private final List<String> occRandomVars;

    private final List<OmegaBlock> omegaBlocksInNonIOV = new ArrayList<OmegaBlock>();
    private final List<OmegaBlock> omegaBlocksInIOV = new ArrayList<OmegaBlock>();

    private final ScriptDefinition scriptDefinition;
    private final ConversionContext context;

    public CorrelationHandler(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null");
        this.context = context;
        Preconditions.checkNotNull(context.getScriptDefinition(), "Script definition cannot be null");
        this.scriptDefinition = context.getScriptDefinition();
        occRandomVars = context.getIovHandler().getOccasionRandomVariables();

        initialise();
    }

    private void initialise(){

        arrangeAllCorrelations();

        OmegaBlock omegaBlockinIOV = new OmegaBlock();
        omegaBlockinIOV.setCorrelations(getIovCorrelations());
        addcorrelationEtas(omegaBlockinIOV, VarLevel.IOV);
        omegaBlocksInIOV.add(omegaBlockinIOV);

        OmegaBlock omegaBlockInNonIOV = new OmegaBlock();
        omegaBlockInNonIOV.setCorrelations(getNonIovCorrelations());
        addcorrelationEtas(omegaBlockInNonIOV, VarLevel.IIV);
        omegaBlocksInNonIOV.add(omegaBlockInNonIOV);

        createAllOrderedEtasMap();
    }

    private void addcorrelationEtas(OmegaBlock omegaBlock, VarLevel varLevel){
        for(CorrelationRef correlation : omegaBlock.getCorrelations()){
            addCorrelationEtas(omegaBlock, varLevel, correlation);
        }
    }

    private Set<Eta> createAllOrderedEtasMap(){

        EtaHandler etaHandler = new EtaHandler(scriptDefinition);
        Set<Eta> allEtas = etaHandler.getAllEtas();
        if(!allEtas.isEmpty()){
            int etaCount = 0;
            List<Eta> nonCorrIOVEtas = new ArrayList<Eta>();
            for(Eta eta : allEtas) {
                boolean isEtaSet = false;
                for(OmegaBlock omegaBlock : omegaBlocksInNonIOV){
                    if(omegaBlock.getEtasToOmegas().keySet().contains(eta)){
                        for(Eta nonIovEta : omegaBlock.getEtasToOmegas().keySet()){
                            if(eta.getEtaSymbol().equals(nonIovEta.getEtaSymbol()) ){
                                nonIovEta.setOrder(++etaCount);
                                nonIovEta.setOrderInCorr(omegaBlock.getOrderedEtas().size()+1);
                                nonIovEta.setVarLevel(VarLevel.IIV);
                                allOrderedEtas.add(nonIovEta);
                                omegaBlock.addToOrderedEtas(nonIovEta);
                                isEtaSet = true;
                            }
                        }
                    }
                }

                if(!isEtaSet){
                    for(OmegaBlock omegaBlock : omegaBlocksInIOV){
                        if(!omegaBlock.getEtasToOmegas().keySet().contains(eta)){
                            if(occRandomVars.contains(eta.getEtaSymbol())){
                                eta.setVarLevel(VarLevel.IOV);
                                eta.setOrderInCorr(1);
                                String etaSymbolForIOV = context.getIovHandler().getColumnWithOcc().getColumnId()+"_"+eta.getOmegaName();
                                eta.setEtaSymbolForIOV(etaSymbolForIOV);

                                nonCorrIOVEtas.add(eta);
                            }else {
                                eta.setVarLevel(VarLevel.IIV);
                                eta.setOrder(++etaCount);
                                allOrderedEtas.add(eta);
                            }
                        }
                    }
                }
            }

            for(OmegaBlock omegaBlock : omegaBlocksInIOV){
                int iovEtaCount = 0;
                for(Eta iovEta : omegaBlock.getEtasToOmegas().keySet()){
                    if(iovEta.getVarLevel().equals(VarLevel.IOV) && iovEta.getOrder()==0){
                        iovEta.setOrder(++etaCount);
                        iovEta.setOrderInCorr(++iovEtaCount);
                        iovEta.setVarLevel(VarLevel.IOV);
                        String etaSymbolForIOV = context.getIovHandler().getColumnWithOcc().getColumnId()+"_"+iovEta.getOmegaName();
                        iovEta.setEtaSymbolForIOV(etaSymbolForIOV);
                        allOrderedEtas.add(iovEta);
                        omegaBlock.addToOrderedEtas(iovEta);
                        omegaBlock.setIsIOV(true);
                    }
                }
            }

            for(Eta nonCorrEta : nonCorrIOVEtas){
                nonCorrEta.setOrder(++etaCount);
                allOrderedEtas.add(nonCorrEta);

                OmegaBlock nonCorrOmegaBlock = new OmegaBlock();
                nonCorrOmegaBlock.addToOrderedEtas(nonCorrEta);
                nonCorrOmegaBlock.addToEtaToOmegas(nonCorrEta, nonCorrEta.getOmegaName());
                nonCorrOmegaBlock.setIsIOV(true);
                omegaBlocksInIOV.add(nonCorrOmegaBlock);
            }
        }
        return allOrderedEtas;
    }

    /**
     * Adds correlations reference to map provided for eta to correlations map.
     * 
     * @param omegaBlocks
     * @param correlation
     */
    private void addCorrelationEtas(OmegaBlock omegaBlock, VarLevel level, CorrelationRef correlation) {
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

        omegaBlock.addToEtaToOmegas(firstEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd1));
        omegaBlock.addToEtaToOmegas(secondEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.rnd2));
        omegaBlock.addToEtaToOmegas(coeffEta,coefficient);
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
     * Collects correlations from all the prameter blocks.
     *  
     */
    private void arrangeAllCorrelations() {
        List<ParameterBlock> parameterBlocks = scriptDefinition.getParameterBlocks();

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

    public List<CorrelationRef> getIovCorrelations() {
        return iovCorrelations;
    }

    public List<CorrelationRef> getNonIovCorrelations() {
        return nonIovCorrelations;
    }

    public List<OmegaBlock> getOmegaBlocksInIOV() {
        return omegaBlocksInIOV;
    }

    public List<OmegaBlock> getOmegaBlocksInNonIOV() {
        return omegaBlocksInNonIOV;
    }
}
