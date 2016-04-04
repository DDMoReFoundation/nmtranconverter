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

    /**
     * Omega block statement creates omega blocks using omega statements using omega statements retrieved from correlations handler,
     * uses paramters initialiser to get omega statements if exists or create omega statement if required and list of unique values in IOV column with help of iov handler.
     *   
     * @param parameters
     * @param correlationHandler
     * @param iovHandler
     */
    public OmegaBlockStatement(ParametersInitialiser parameters, CorrelationHandler correlationHandler, InterOccVariabilityHandler iovHandler) {
        Preconditions.checkNotNull(parameters, "parameter should not be null");
        Preconditions.checkNotNull(correlationHandler, "Correlation handler should not be null");
        Preconditions.checkNotNull(iovHandler, "IOV handler should not be null");

        this.parameters = parameters;
        this.iovHandler = iovHandler;
        this.correlationHandler = correlationHandler;
    }

    /**
     * This method creates omega blocks if there are any.
     * We uses ordered etas and eta to omega map to determine order of the omega block elements.
     * Currently only correlations are supported.
     */
    public void createOmegaBlocks(){
        OmegaBlockPopulator blockCreator = new OmegaBlockPopulator(parameters, iovHandler.getIovColumnUniqueValues());

        for(OmegaBlock omegaBlock : correlationHandler.getOmegaBlocksInNonIOV()){
            if(omegaBlock.getOrderedEtas().size()>0){
                blockCreator.populateOmegaBlock(omegaBlock);
            }
        }
        for(OmegaBlock omegaBlock : correlationHandler.getOmegaBlocksInIOV()){
            if(omegaBlock.getOrderedEtas().size()>0){
                blockCreator.populateOmegaBlock(omegaBlock);
            }
        }
    }
}
