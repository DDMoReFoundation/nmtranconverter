/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.utils.Formatter;

public class NonDerivativePredStatementTest extends BasicTestSetup {

    @Mock ModelStatementHelper statementHelper;
    @Mock PredCoreStatement predCoreStatement;
    @Mock ErrorStatementHandler errorStatementHandler;

    private static final String EXPECTED_OUTPUT = Formatter.endline()+Formatter.endline()+Formatter.pred()+
            PRED_EXAMPLE+ IDV_EXAMPLE+ Formatter.endline(VAR_DEF_EXAMPLE)+ERROR_EXAMPLE;

    @Before
    public void setUp() throws Exception {

        when(predCoreStatement.getStatement()).thenReturn(new StringBuilder(PRED_EXAMPLE));
        when(statementHelper.getAllIndividualParamAssignments()).thenReturn(new StringBuilder(IDV_EXAMPLE));
        when(statementHelper.getPredCoreStatement()).thenReturn(predCoreStatement);
        when(statementHelper.getVarDefinitionTypesForNonDES()).thenReturn(new StringBuilder(VAR_DEF_EXAMPLE));
        when(statementHelper.getErrorStatementHandler()).thenReturn(errorStatementHandler);
        when(errorStatementHandler.getErrorStatement()).thenReturn(ERROR_EXAMPLE);
    }

    @Test
    public void shouldGetPredStatement() {
        NonDerivativePredStatement predStatement = new NonDerivativePredStatement(statementHelper);
        StringBuilder predStatementOutput = predStatement.getPredStatement();

        assertNotNull("Pred statement should not be null", predStatementOutput);
        assertEquals("Should return pred statement as expected.", EXPECTED_OUTPUT, predStatementOutput.toString());
    }

}
