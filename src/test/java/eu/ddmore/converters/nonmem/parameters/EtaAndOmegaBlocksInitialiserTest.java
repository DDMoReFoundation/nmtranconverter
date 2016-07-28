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

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.EtaHandler;

@PrepareForTest(EtaAndOmegaBlocksInitialiser.class)
public class EtaAndOmegaBlocksInitialiserTest extends ParametersMockHelper {

    @Mock
    EtaHandler etaHandler;

    EtaAndOmegaBlocksInitialiser blocksInitialiser;
    List<OmegaBlock> omegaBlocksInNonIOV;
    List<OmegaBlock> omegaBlocksInIOV;

    @Before
    public void setUp() throws Exception {
        createDummyOmegaBlockToPopulate();
        omegaBlocksInIOV = new ArrayList<OmegaBlock>();
        omegaBlocksInNonIOV = new ArrayList<OmegaBlock>();
        omegaBlocksInIOV.add(omegaBlock);
        omegaBlocksInNonIOV.add(omegaBlock);

        etas = new HashSet<>();
        etas.add(firstEta);
        etas.add(secondEta);
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);
        whenNew(EtaHandler.class).withArguments(scriptDefinition).thenReturn(etaHandler);
        when(etaHandler.getAllEtas()).thenReturn(etas);
    }

    @Test
    public void shouldReturnPopulateOrderedEtaAndOmegaBlocks() {

        blocksInitialiser = new EtaAndOmegaBlocksInitialiser(context, omegaBlocksInIOV, omegaBlocksInNonIOV);
        Set<Eta> etas = blocksInitialiser.populateOrderedEtaAndOmegaBlocks();
        assertNotNull("Etas should not be null", etas);
        assertTrue("Returned etas should contains expected eta", etas.contains(firstEta));
    }

}
