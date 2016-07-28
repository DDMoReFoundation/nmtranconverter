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

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.statements.input.InterOccVariabilityHandler;
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
     */
    public OmegaBlockStatement(ParametersInitialiser parametersInitialiser, CorrelationHandler correlationHandler, InterOccVariabilityHandler iovHandler) {
        Preconditions.checkNotNull(parametersInitialiser, "parameter should not be null");
        Preconditions.checkNotNull(correlationHandler, "Correlation handler should not be null");
        Preconditions.checkNotNull(iovHandler, "IOV handler should not be null");

        this.parameters = parametersInitialiser;
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
