/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import eu.ddmore.converters.nonmem.utils.Formatter;

public class SigmaStatementBuilderTest extends ParametersMockHelper {

    private final String expectedsigmaStatement = Formatter.endline()+Formatter.sigma()+Formatter.endline("1.0 FIX"); 
    @Before
    public void setUp() throws Exception {
        mockRandomVarHelper();
        epsilonVars = new HashSet<>();
        epsilonVars.add(firstPrv);
    }

    @Test
    public void shouldGetSigmaStatementBlock() {
        SigmaStatementBuilder sigmaStatementBuilder = new SigmaStatementBuilder(context, epsilonVars);
        assertNotNull("Should get sigma statements block", sigmaStatementBuilder.getSigmaStatementBlock());
        assertEquals("Should get expected sigma statement.", expectedsigmaStatement, sigmaStatementBuilder.getSigmaStatementBlock().toString());
    }
}
