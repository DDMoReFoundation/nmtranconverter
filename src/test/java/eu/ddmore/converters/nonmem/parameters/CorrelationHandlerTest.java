/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
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
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
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
