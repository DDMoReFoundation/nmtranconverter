/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.common.CorrelationRef;
import crx.converter.spi.blocks.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VariabilityLevel;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;

/**
 * This class deals with ordered etas with help of correlations specified. 
 * Also it arranges eta to correlations mapping as well.
 */
public class CorrelationHandler {

    //Either we need to move etas from here or rename class as it does correlation omega and IOV omega initialisation
    private Set<Eta> allOrderedEtas;

    private final List<CorrelationsWrapper> iovCorrelations = new ArrayList<CorrelationsWrapper>();
    private final List<CorrelationsWrapper> nonIovCorrelations = new ArrayList<CorrelationsWrapper>();
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
        OmegaBlock omegaBlockInIOV = new OmegaBlock();
        omegaBlockInIOV.setCorrelations(getIovCorrelations());

        addcorrelationEtas(omegaBlockInIOV, VariabilityLevel.IOV);
        omegaBlocksInIOV.add(omegaBlockInIOV);

        OmegaBlock omegaBlockInNonIOV = new OmegaBlock();
        omegaBlockInNonIOV.setCorrelations(getNonIovCorrelations());
        addcorrelationEtas(omegaBlockInNonIOV, VariabilityLevel.IIV);
        omegaBlocksInNonIOV.add(omegaBlockInNonIOV);

        EtaAndOmegaBlocksInitialiser blocksInitialiser = new EtaAndOmegaBlocksInitialiser(context, omegaBlocksInIOV, omegaBlocksInNonIOV);
        allOrderedEtas = blocksInitialiser.populateOrderedEtaAndOmegaBlocks();
    }

    private void addcorrelationEtas(OmegaBlock omegaBlock, VariabilityLevel varLevel){
        for(CorrelationsWrapper correlation : omegaBlock.getCorrelations()){
            addCorrelationEtas(omegaBlock, varLevel, correlation);
        }
    }

    private void addCorrelationEtas(OmegaBlock omegaBlock, VariabilityLevel level, CorrelationsWrapper correlation) {
        Preconditions.checkNotNull(correlation, "Correlation reference cannot be null");

        String firstVar = correlation.getFirstParamRandomVariable().getSymbId();
        String secondVar = correlation.getSecondParamRandomVariable().getSymbId();
        String coefficient = "";
        if(correlation.isCorrelationCoeff()){
            coefficient = getVariableOrValueFromRhs(correlation.getCorrelationCoefficient().getAssign());
        }else if(correlation.isCovariance()){
            coefficient = getVariableOrValueFromRhs(correlation.getCovariance().getAssign()).toString();
        }
        //add to correlations
        Eta firstEta = new Eta(firstVar);
        firstEta.setVarLevel(level);
        firstEta.setCorrelationRelated(true);
        firstEta.setOmegaName(RandomVariableHelper.getNameFromParamRandomVariable(correlation.getFirstParamRandomVariable()));

        Eta secondEta = new Eta(secondVar);
        secondEta.setVarLevel(level);
        secondEta.setCorrelationRelated(true);
        secondEta.setOmegaName(RandomVariableHelper.getNameFromParamRandomVariable(correlation.getSecondParamRandomVariable()));

        Eta coeffEta = new Eta(coefficient);
        coeffEta.setOmegaName(coefficient);
        coeffEta.setVarLevel(VariabilityLevel.NONE);

        omegaBlock.addToEtaToOmegas(firstEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.getFirstParamRandomVariable()));
        omegaBlock.addToEtaToOmegas(secondEta,RandomVariableHelper.getNameFromParamRandomVariable(correlation.getSecondParamRandomVariable()));
        omegaBlock.addToEtaToOmegas(coeffEta,coefficient);
    }

    /**
     * Gets variable from scalar rhs if it exists or else looks for scalar value and returns in string form.
     *    
     * @param rhs
     * @return scalar variable or value
     */
    private String getVariableOrValueFromRhs(Rhs rhs) {
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
                    CorrelationsWrapper correlation = new CorrelationsWrapper(itr.next());
                    boolean isIOV = false;
                    if(occRandomVars.contains(correlation.getFirstParamRandomVariable().getSymbId()) 
                            || occRandomVars.contains(correlation.getSecondParamRandomVariable().getSymbId())){
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

    public List<CorrelationsWrapper> getIovCorrelations() {
        return iovCorrelations;
    }

    public List<CorrelationsWrapper> getNonIovCorrelations() {
        return nonIovCorrelations;
    }

    public List<OmegaBlock> getOmegaBlocksInIOV() {
        return omegaBlocksInIOV;
    }

    public List<OmegaBlock> getOmegaBlocksInNonIOV() {
        return omegaBlocksInNonIOV;
    }
}
