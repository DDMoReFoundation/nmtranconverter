/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VariabilityLevel;
import eu.ddmore.converters.nonmem.statements.input.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.utils.EtaHandler;

/**
 * Initialises ordered Etas and omegas.
 */
public class EtaAndOmegaBlocksInitialiser {

    private static final int DEFAULT_CORR_ORDER = 1;
    private final Set<Eta> allOrderedEtas = new TreeSet<Eta>();
    private final List<OmegaBlock> omegaBlocksInNonIOV;
    private final List<OmegaBlock> omegaBlocksInIOV;
    private final ConversionContext conversionContext;
    private final InterOccVariabilityHandler iovhandler;

    public EtaAndOmegaBlocksInitialiser(ConversionContext context, List<OmegaBlock> omegaBlocksInIOV, List<OmegaBlock> omegaBlocksInNonIOV){
        Preconditions.checkNotNull(context, "conversion context cannot be null");
        Preconditions.checkNotNull(omegaBlocksInIOV, "IOV omega blocks list cannot be null");
        Preconditions.checkNotNull(omegaBlocksInNonIOV, "Non-IOV omega blocks list cannot be null");
        this.conversionContext = context;
        this.omegaBlocksInIOV = omegaBlocksInIOV;
        this.omegaBlocksInNonIOV = omegaBlocksInNonIOV;
        this.iovhandler = context.getIovHandler();
    }

    /**
     * Populate ordered etas and omega blocks from eta handler.
     * @return all etas ordered
     */
    public Set<Eta> populateOrderedEtaAndOmegaBlocks(){

        EtaHandler etaHandler = new EtaHandler(conversionContext.getScriptDefinition());
        Set<Eta> allEtas = etaHandler.getAllEtas();

        if(!allEtas.isEmpty()){
            int etaCount = 0;
            List<Eta> nonCorrIOVEtas = new ArrayList<Eta>();

            for(Eta eta : allEtas) {
                etaCount = addEtaToOmegaBlock(etaCount, nonCorrIOVEtas, eta);
            }
            etaCount = addIOVEtasToOmegaBlock(etaCount);
            etaCount = addNonCorrIOVEtasToOmegaBlock(etaCount, nonCorrIOVEtas);
        }
        return allOrderedEtas;
    }

    private int addIOVEtasToOmegaBlock(int etaCount) {

        for(OmegaBlock omegaBlock : omegaBlocksInIOV){
            int iovEtaCount = 0;
            for(Eta iovEta : omegaBlock.getOmegaBlockEtas()){
                if(iovEta.getVariabilityLevel().equals(VariabilityLevel.IOV) && iovEta.getOrder()==0){
                    iovEta.setOrder(++etaCount);
                    iovEta.setOrderInCorr(++iovEtaCount);
                    iovEta.setVariabilityLevel(VariabilityLevel.IOV);
                    String etaSymbolForIOV = iovhandler.getIovColumn().getColumnId()+"_"+iovEta.getOmegaName();
                    iovEta.setEtaSymbolForIOV(etaSymbolForIOV);
                    allOrderedEtas.add(iovEta);
                    omegaBlock.addToOrderedEtas(iovEta);
                    omegaBlock.setIsIOV(true);
                }
            }
        }
        return etaCount;
    }

    private int addNonCorrIOVEtasToOmegaBlock(int etaCount, List<Eta> nonCorrIOVEtas) {

        for(Eta nonCorrEta : nonCorrIOVEtas){
            nonCorrEta.setOrder(++etaCount);
            allOrderedEtas.add(nonCorrEta);

            OmegaBlock nonCorrOmegaBlock = new OmegaBlock();
            nonCorrOmegaBlock.addToOrderedEtas(nonCorrEta);
            nonCorrOmegaBlock.addToOmegaBlockEtas(nonCorrEta);
            nonCorrOmegaBlock.setIsIOV(true);
            omegaBlocksInIOV.add(nonCorrOmegaBlock);
        }
        return etaCount;
    }

    private int addEtaToOmegaBlock(int etaCount, List<Eta> nonCorrIOVEtas, Eta eta) {
        boolean isEtaSet = false;

        for(OmegaBlock omegaBlock : omegaBlocksInNonIOV){
            if(omegaBlock.getOmegaBlockEtas().contains(eta)){
                for(Eta nonIovEta : omegaBlock.getOmegaBlockEtas()){
                    if(eta.getEtaSymbol().equals(nonIovEta.getEtaSymbol()) ){
                        nonIovEta.setOrder(++etaCount);
                        nonIovEta.setOrderInCorr(omegaBlock.getOrderedEtas().size()+1);
                        nonIovEta.setVariabilityLevel(VariabilityLevel.IIV);
                        allOrderedEtas.add(nonIovEta);
                        omegaBlock.addToOrderedEtas(nonIovEta);
                        isEtaSet = true;
                    }
                }
            }
        }

        if(!isEtaSet){
            etaCount = addEtaToNonCorrIOVEtas(etaCount, nonCorrIOVEtas, eta);
        }

        return etaCount;
    }

    private int addEtaToNonCorrIOVEtas(int etaCount, List<Eta> nonCorrIOVEtas, Eta eta) {

        for(OmegaBlock omegaBlock : omegaBlocksInIOV){
            if(!omegaBlock.getOmegaBlockEtas().contains(eta)){
                if(iovhandler.getOccasionRandomVariables().contains(eta.getEtaSymbol())){
                    eta.setVariabilityLevel(VariabilityLevel.IOV);
                    eta.setOrderInCorr(DEFAULT_CORR_ORDER);
                    String etaSymbolForIOV = iovhandler.getIovColumn().getColumnId()+"_"+eta.getOmegaName();
                    eta.setEtaSymbolForIOV(etaSymbolForIOV);

                    nonCorrIOVEtas.add(eta);
                }else {
                    eta.setVariabilityLevel(VariabilityLevel.IIV);
                    eta.setOrder(++etaCount);
                    allOrderedEtas.add(eta);
                }
            }
        }
        return etaCount;
    }
}
