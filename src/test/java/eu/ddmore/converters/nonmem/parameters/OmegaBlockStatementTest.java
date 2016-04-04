/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;


public class OmegaBlockStatementTest extends ParametersMockHelper {

    @Mock OmegaBlockPopulator omegaBlockCreator;

    OmegaBlockStatement omegaBlockStatement;

    List<OmegaBlock> omegaBlocks = new ArrayList<>();
    List<Double> uniqueValues = new ArrayList<>();
    @Before
    public void setUp() throws Exception {

        initialiseDummyEtasInCorrelation();
        initialiseDummyOmegaBlock();

        uniqueValues.add(new Double(1));
        uniqueValues.add(new Double(2));

        etas = new HashSet<>();
        etas.add(firstEta);
        etas.add(secondEta);
        omegaBlocks.add(omegaBlock);
        when(correlationHandler.getOmegaBlocksInIOV()).thenReturn(omegaBlocks);
        whenNew(OmegaBlockPopulator.class).withArguments(paramInitialiser, uniqueValues).thenReturn(omegaBlockCreator);
    }

    @Test
    public void testCreateOmegaBlocksForNonIOV() {
        omegaBlockStatement = new OmegaBlockStatement(paramInitialiser,correlationHandler,iovHandler);
        omegaBlockStatement.createOmegaBlocks();
        assertEquals("should return omega title", expectedOmegaTitle, omegaBlock.getOmegaBlockTitle().trim());
    }

    private void mockOmegaBlocksForIOV(){

        when(iovHandler.getIovColumnUniqueValues()).thenReturn(uniqueValues);

        omegaBlock.setIsIOV(true);
    }

    @Test
    public void testCreateOmegaBlocksForIOV() {
        mockOmegaBlocksForIOV();
        final String expectedOmegaSameTitle = expectedOmegaTitle+" SAME"; 
        omegaBlockStatement = new OmegaBlockStatement(paramInitialiser,correlationHandler,iovHandler);
        omegaBlockStatement.createOmegaBlocks();
        assertEquals("should return omega title", expectedOmegaTitle, omegaBlock.getOmegaBlockTitle().trim());
        assertEquals("should return omega SAME title", expectedOmegaSameTitle, omegaBlock.getOmegaBlockSameTitle().trim());
    }

}
