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

import java.util.List;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;

/**
 * This class initialises creates omega block and omega title as well as same block title for IOV.
 */
public class OmegaBlockPopulator {
    private final ParametersInitialiser parameters;
    private final List<Double> iovColumnUniqueValues;

    public OmegaBlockPopulator(ParametersInitialiser parametersInitialiser, List<Double> iovColumnUniqueValues){
        Preconditions.checkNotNull(parametersInitialiser, "parameters initialiser cannot be null");
        Preconditions.checkNotNull(iovColumnUniqueValues, "List of unique values in iov column cannot be null");
        this.parameters = parametersInitialiser;
        this.iovColumnUniqueValues = iovColumnUniqueValues;
    }

    /**
     * Populate omega block provided with help of parameter initialiser
     * @param omegaBlock
     */
    public void populateOmegaBlock(OmegaBlock omegaBlock){
        Preconditions.checkNotNull(omegaBlock, "Omega block cannot be null");

        OmegaBlockInitialiser blockInitialiser = new OmegaBlockInitialiser();
        blockInitialiser.initialiseOmegaBlock(omegaBlock);

        OmegaStatementsMatrixPopulator statementsPopulator = new OmegaStatementsMatrixPopulator();
        statementsPopulator.populateOmegaStatementMatrixForOmegaBlock(omegaBlock, parameters);

        createOmegaBlockTitle(omegaBlock);
        createOmegaSameBlockTitle(omegaBlock);
    }

    private void createOmegaBlockTitle(OmegaBlock omegaBlock) {
        StringBuilder description = new StringBuilder();
        //This will change in case of 0.4 as it will need to deal with matrix types as well.
        description.append((omegaBlock.isCorrelation())?NmConstant.CORRELATION:" ");
        description.append((omegaBlock.isOmegaBlockFromStdDev())?" "+NmConstant.SD:"");
        String title = String.format(Formatter.endline()+"%s %s", Formatter.omegaBlock(omegaBlock.getOrderedEtas().size()),description);
        omegaBlock.setOmegaBlockTitle(title);
    }

    private void createOmegaSameBlockTitle(OmegaBlock omegaBlock) {
        if(omegaBlock.isIOV()){
            Integer uniqueValueCount = iovColumnUniqueValues.size();
            Integer blockCount = omegaBlock.getOrderedEtas().size();
            String title = String.format(Formatter.endline()+"%s", Formatter.omegaSameBlock(blockCount, uniqueValueCount));
            omegaBlock.setOmegaBlockSameTitle(title);
        }
    }
}
