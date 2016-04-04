/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
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
    private OmegaBlock omegaBlock;

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
        this.omegaBlock = omegaBlock;

        OmegaBlockInitialiser blockInitialiser = new OmegaBlockInitialiser(omegaBlock);
        blockInitialiser.initialiseOmegaBlock();

        OmegaStatementsMatrixPopulator statementsPopulator = new OmegaStatementsMatrixPopulator(omegaBlock, parameters);
        statementsPopulator.createOmegaStatementMatrixForOmegaBlock();

        createOmegaBlockTitle();
        createOmegaSameBlockTitle();
    }

    private void createOmegaBlockTitle() {
        StringBuilder description = new StringBuilder();
        //This will change in case of 0.4 as it will need to deal with matrix types as well.
        description.append((omegaBlock.isCorrelation())?NmConstant.CORRELATION:" ");
        description.append((omegaBlock.isOmegaBlockFromStdDev())?" "+NmConstant.SD:"");
        String title = String.format(Formatter.endline()+"%s %s", Formatter.omegaBlock(omegaBlock.getOrderedEtas().size()),description);
        omegaBlock.setOmegaBlockTitle(title);
    }

    private void createOmegaSameBlockTitle() {
        if(omegaBlock.isIOV()){
            Integer uniqueValueCount = iovColumnUniqueValues.size();
            Integer blockCount = omegaBlock.getOrderedEtas().size();
            String title = String.format(Formatter.endline()+"%s", Formatter.omegaSameBlock(blockCount, uniqueValueCount));
            omegaBlock.setOmegaBlockSameTitle(title);
        }
    }
}
