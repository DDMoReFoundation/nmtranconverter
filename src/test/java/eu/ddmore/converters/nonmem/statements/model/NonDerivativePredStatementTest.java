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
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.error.ErrorStatementHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Junit tests for NonDerivativePredStatement class.
 */
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
