/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Junit test for cov statement builder class 
 */
public class CovStatementBuilderTest extends BasicTestSetup {

    CovStatementBuilder covStatementBuilder;

    @Before
    public void setUp() throws Exception {
        covStatementBuilder = new CovStatementBuilder();
        covStatementBuilder.setCovFound(true);
    }

    @Test
    public void shouldGetCovStatement() {
        covStatementBuilder.buildCovStatement();
        String covStatement = covStatementBuilder.getCovStatement().toString();
        assertNotNull("Cov statement cannot be null if operation type is estFIM", covStatement);

        assertFalse("Cov statement is expected here.", covStatement.trim().isEmpty());
        assertEquals("Should return correct statement", Formatter.cov().trim(), covStatement.trim().toString());
    }

    @Test
    public void shouldGetCovStatementWithOptions() {
        covStatementBuilder.getCovOptions().put(COL_ID_3, COL_NUM_3.toString());
        covStatementBuilder.getCovOptions().put(COL_ID_4, COL_NUM_4.toString());

        covStatementBuilder.buildCovStatement();

        String optionStatement =  COL_ID_3+"="+COL_NUM_3+" "+COL_ID_4+"="+COL_NUM_4;
        String expectedStatement = Formatter.cov()+" "+optionStatement;
        String covStatement = covStatementBuilder.getCovStatement().toString();
        assertNotNull("Cov statement cannot be null if operation type is estFIM", covStatement);

        assertFalse("Cov statement is expected here.", covStatement.trim().isEmpty());
        assertEquals("Should return correct statement", expectedStatement.trim(), covStatement.trim().toString());
    }
}
