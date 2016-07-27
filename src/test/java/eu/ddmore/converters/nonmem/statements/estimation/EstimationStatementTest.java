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
package eu.ddmore.converters.nonmem.statements.estimation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationDetailsEmitter;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;

public class EstimationStatementTest extends BasicTestSetup {

    @Mock EstimationDetailsEmitter estimationEmitter;

    EstimationStatement estStatement;

    String statementExample = "$EST METHOD=SAEM AUTO=1 PRINT=100 CINTERVAL=30 ATOL=6 SIGL=6";
    StringBuilder statementDetail = new StringBuilder(Formatter.endline()+statementExample);
    String simContent = "sim block content";
    String outputStatement = Formatter.endline()+simContent+Formatter.endline()+
            statementExample+Formatter.endline()+
            Formatter.cov()+simContent;

    @Before
    public void setUp() throws Exception {
        when(estimationEmitter.getEstimationStatement()).thenReturn(statementDetail);
        when(estimationEmitter.getCovStatement()).thenReturn(Formatter.endline()+Formatter.cov());
        when(estimationEmitter.addSimContentForDiscrete(Mockito.anyString())).thenReturn(simContent);
    }

    @Test
    public void shouldGetStatementsWithEstimationDetails() {
        estStatement = new EstimationStatement(estimationEmitter);
        String outputEstStatement = estStatement.getStatementsWithEstimationDetails();

        assertNotNull("estimation statement is not null", outputEstStatement);
        assertEquals("Should return expected estimation statement", outputStatement, outputEstStatement);
    }

}
