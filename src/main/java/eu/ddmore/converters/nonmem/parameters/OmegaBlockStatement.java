/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.parameters.CorrelationHandler;

/**
 * This class builds omega block statement and related information.
 */
public class OmegaBlockStatement {

    private final CorrelationHandler correlationHandler;
    private final InterOccVariabilityHandler iovHandler;
    private final ParametersInitialiser parameters;

    public OmegaBlockStatement(ParametersInitialiser parameters, CorrelationHandler correlationHandler, InterOccVariabilityHandler iovHandler) {
        Preconditions.checkNotNull(parameters, "parameter should not be null");
        Preconditions.checkNotNull(correlationHandler, "Correlation handler should not be null");
        Preconditions.checkNotNull(iovHandler, "IOV handler should not be null");

        this.parameters = parameters;
        this.iovHandler = iovHandler;
        this.correlationHandler = correlationHandler;
    }

    /**
     * This method will create omega blocks if there are any.
     * We will need ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     *  
     */
    public void createOmegaBlocks(){

        for(OmegaBlock omegaBlock : correlationHandler.getOmegaBlocksInNonIOV()){
            if(omegaBlock.getOrderedEtas().size()>0){
                OmegaBlockCreator blockCreator = new OmegaBlockCreator(parameters, iovHandler, omegaBlock);
                blockCreator.initialiseOmegaBlocks(omegaBlock);
                blockCreator.createOmegaBlocks(omegaBlock);
                blockCreator.createOmegaBlockTitle(omegaBlock);
            }
        }

        for(OmegaBlock omegaBlock : correlationHandler.getOmegaBlocksInIOV()){
            OmegaBlockCreator blockCreator = new OmegaBlockCreator(parameters, iovHandler, omegaBlock);
            blockCreator.initialiseOmegaBlocks(omegaBlock);
            blockCreator.createOmegaBlocks(omegaBlock);
            blockCreator.createOmegaBlockTitle(omegaBlock);
            blockCreator.createOmegaSameBlockTitle(omegaBlock);
        }
    }
}
