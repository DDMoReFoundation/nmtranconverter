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
