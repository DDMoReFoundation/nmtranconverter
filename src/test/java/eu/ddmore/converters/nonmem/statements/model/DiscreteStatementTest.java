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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.error.ErrorStatementHandler;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Junit tests for DiscreteStatement class. 
 */
public class DiscreteStatementTest extends BasicTestSetup {

    private static final String EXAMPLE_STRING = Formatter.endline("PRED CORE");
    private static final String HAZ_VAR = "HAZ_VAR";
    @Mock DiscreteHandler discreteHandler;
    @Mock ModelStatementHelper modelStatementHelper;
    @Mock DiffEquationStatementBuilder desBuilder;
    @Mock PredCoreStatement predCoreStatement;
    @Mock ErrorStatementHandler errorStatementHandler; 

    private Map<String, String> varDefs = new HashMap<String, String>();
    private DiscreteStatement discreteStatement;

    @Before
    public void setUp() throws Exception {

        discreteHandler = mock(DiscreteHandler.class, RETURNS_DEEP_STUBS);

        StringBuilder specimenStringBuilder = new StringBuilder(EXAMPLE_STRING);
        when(context.getDiscreteHandler()).thenReturn(discreteHandler);
        when(discreteHandler.getHazardFunction()).thenReturn(HAZ_VAR);
        when(discreteHandler.getDiscreteStatement()).thenReturn(specimenStringBuilder);

        mockModelStatementHelper();
    }

    private void mockModelStatementHelper(){
        StringBuilder specimenStringBuilder = new StringBuilder(EXAMPLE_STRING);
        when(desBuilder.getVariableDefinitionsStatement(varDefs)).thenReturn(new StringBuilder(VAR_DEF_EXAMPLE));

        when(predCoreStatement.getStatement()).thenReturn(new StringBuilder(PRED_EXAMPLE));
        when(modelStatementHelper.getAllIndividualParamAssignments()).thenReturn(new StringBuilder(IDV_EXAMPLE));
        when(modelStatementHelper.getPredCoreStatement()).thenReturn(predCoreStatement);
        when(modelStatementHelper.getDiffEquationStatement((StringBuilder) Matchers.any())).thenReturn(desBuilder);
        when(modelStatementHelper.getVarDefinitionTypesForNonDES()).thenReturn(specimenStringBuilder);
        when(modelStatementHelper.getErrorStatementHandler()).thenReturn(errorStatementHandler);
        when(errorStatementHandler.getErrorStatement()).thenReturn(ERROR_EXAMPLE);
    }
    @Test
    public void shouldGetModelStatementForCountData() {
        discreteStatement = new DiscreteStatement(modelStatementHelper);
        assertNotNull("Discrete statement should not be null", discreteStatement.getModelStatementForCountData(discreteHandler));
    }

    @Test
    public void shouldGetModelStatementForTTE() {

        String hazardFunc = "HAZARD_FUNC = HAZ_VAR";
        discreteStatement = new DiscreteStatement(modelStatementHelper);

        assertNotNull("Discrete statement should not be null", discreteStatement.buildModelStatementForTTE(discreteHandler));
        assertTrue("Should contain hazard function", discreteStatement.buildModelStatementForTTE(discreteHandler).toString().contains(hazardFunc));

    }

}
