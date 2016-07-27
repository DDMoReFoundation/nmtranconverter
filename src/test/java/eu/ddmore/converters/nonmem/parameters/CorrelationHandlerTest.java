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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.spi.blocks.ParameterBlock;

import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.input.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.utils.EtaHandler;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

@PrepareForTest(EtaAndOmegaBlocksInitialiser.class)
public class CorrelationHandlerTest extends BasicTestSetup {

    @Mock InterOccVariabilityHandler iovHandler;
    @Mock EtaHandler etaHandler;
    @Mock ParameterBlock parameterBlock;
    @Mock ParameterRandomVariable firstRandomVar;
    @Mock ParameterRandomVariable secondRandomVar;

    private final Eta eta = new Eta("ETA1");

    @Before
    public void setUp() throws Exception {
        Set<Eta> etas = new HashSet<>();
        List<String> occRandomVars = new ArrayList<>();
        occRandomVars.add(COL_ID_1);
        occRandomVars.add(COL_ID_2);

        etas.add(eta);
        when(context.getIovHandler()).thenReturn(iovHandler);
        when(iovHandler.getOccasionRandomVariables()).thenReturn(occRandomVars);

        whenNew(EtaHandler.class).withArguments(scriptDefinition).thenReturn(etaHandler);
        when(etaHandler.getAllEtas()).thenReturn(etas);
    }

    @Test
    public void shouldGetAllOrderedEtas() {
        CorrelationHandler correlationHandler = new CorrelationHandler(context);

        assertNotNull("ordered etas should not be null", correlationHandler.getAllOrderedEtas());
        for(Eta nextEta : correlationHandler.getAllOrderedEtas()){
            assertEquals("Should get ordered Eta", eta.getEtaSymbol(), nextEta.getEtaSymbol());
        }
    }
}
