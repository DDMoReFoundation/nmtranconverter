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
