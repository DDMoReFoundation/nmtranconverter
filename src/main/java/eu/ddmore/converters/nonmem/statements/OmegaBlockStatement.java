/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.CorrelationHandler;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;

public class OmegaBlockStatement {

    private final ParametersHelper paramHelper;
    private final CorrelationHandler orderedEtasHandler;
    private final InterOccVariabilityHandler iovHandler;

    public OmegaBlockStatement(ParametersHelper parameter, CorrelationHandler orderedEtasHandler, InterOccVariabilityHandler iovHandler) {
        Preconditions.checkNotNull(parameter, "parameter should not be null");
        Preconditions.checkNotNull(orderedEtasHandler, "ordered etas handler should not be null");
        Preconditions.checkNotNull(iovHandler, "IOV handler should not be null");

        this.paramHelper = parameter;
        this.iovHandler = iovHandler;
        this.orderedEtasHandler = orderedEtasHandler;
    }

    /**
     * This method will create omega blocks if there are any.
     * We will need ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     *  
     */
    public void createOmegaBlocks(){

        for(OmegaBlock omegaBlock : orderedEtasHandler.getOmegaBlocksInNonIOV()){
            if(omegaBlock.getOrderedEtas().size()>0){
                OmegaBlockCreator blockCreator = new OmegaBlockCreator(paramHelper, iovHandler, omegaBlock);
                blockCreator.initialiseOmegaBlocks(omegaBlock);
                blockCreator.createOmegaBlocks(omegaBlock);
                blockCreator.createOmegaBlockTitle(omegaBlock);
            }
        }

        for(OmegaBlock omegaBlock : orderedEtasHandler.getOmegaBlocksInIOV()){
            OmegaBlockCreator blockCreator = new OmegaBlockCreator(paramHelper, iovHandler, omegaBlock);
            blockCreator.initialiseOmegaBlocks(omegaBlock);
            blockCreator.createOmegaBlocks(omegaBlock);
            blockCreator.createOmegaBlockTitle(omegaBlock);
            blockCreator.createOmegaSameBlockTitle(omegaBlock);
        }

    }

    public List<OmegaBlock> getOmegaBlocksInNonIOV() {
        return orderedEtasHandler.getOmegaBlocksInNonIOV();
    }

    public List<OmegaBlock> getOmegaBlocksInIOV() {
        return orderedEtasHandler.getOmegaBlocksInIOV();
    }

}
