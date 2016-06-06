/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;

@PrepareForTest(RandomVariableHelper.class)
public class OmegaBlockCreatorTest extends ParametersMockHelper {

    @Before
    public void setUp() throws Exception {
        createDummyOmegaBlockToPopulate();
    }

    @Test
    public void shouldInitialiseOmegaBlocks() {
        OmegaBlockPopulator blockCreator = new OmegaBlockPopulator(paramInitialiser, iovHandler.getIovColumnUniqueValues());
        blockCreator.populateOmegaBlock(omegaBlock);

        assertFalse("Omega statements map should not be empty", omegaBlock.getOrderedEtas().isEmpty());
    }

    @Test
    public void shouldCreateOmegaBlocks(){
        OmegaBlockPopulator blockCreator = new OmegaBlockPopulator(paramInitialiser, iovHandler.getIovColumnUniqueValues());
        blockCreator.populateOmegaBlock(omegaBlock);

        for(Eta eta :omegaBlock.getOrderedEtas()){
            List<OmegaParameter> omegas = eta.getOmegaParameters();
            if(eta.getEtaSymbol().equals(firstEta.getEtaSymbol())){
                assertTrue("First row of omega block should have one omega",omegas.size()==1);
            }
            if(eta.getEtaSymbol().equals(secondEta.getEtaSymbol())){
                assertTrue("First row of omega block should have two omegas",omegas.size()==2);
                OmegaParameter coeffOmega = omegas.get(0);
                assertEquals("First omega should be manually created omega if corresponding correlation coefficient is not provided.", "Empty Variable", coeffOmega.getSymbId());
            }
        }
    }

    @Test
    public void shouldCreateOmegaBlockTitle(){
        omegaBlock.setIsCorrelation(true);

        OmegaBlockPopulator blockCreator = new OmegaBlockPopulator(paramInitialiser, iovHandler.getIovColumnUniqueValues());
        blockCreator.populateOmegaBlock(omegaBlock);
        assertEquals("should return omega title", expectedOmegaTitle+" "+Formatter.NmConstant.CORRELATION, omegaBlock.getOmegaBlockTitle().trim());
    }

}
