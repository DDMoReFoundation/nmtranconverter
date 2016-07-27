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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationStatementBuilder.EstConstant;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationStatementBuilder.Method;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.modellingsteps.Algorithm;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;


/**
 * Junit test for estimation statement builder class 
 */
public class EstimationStatementBuilderTest extends BasicTestSetup {

    private static final String METHOD = "METHOD=";
    @Mock DiscreteHandler discreteHandler;

    EstimationStatementBuilder estStatementBuilder;

    Map<String, String> estOptions = new LinkedHashMap<String, String>();
    @Mock EstimationOperation estOperation;
    @Mock Algorithm algorithm;

    @Test
    public void shouldGetEstimationStatementForFO() {

        String methodType = Method.FO.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FO.getMethod()+" "+EstConstant.FO.getStatement();

        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementWithOptions() {

        estOptions.put(COL_ID_3, COL_NUM_3.toString());
        estOptions.put(COL_ID_4, COL_NUM_4.toString());
        String methodType = Method.FO.toString();
        String optionStatement =  " "+COL_ID_3+"="+COL_NUM_3+" "+COL_ID_4+"="+COL_NUM_4;
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FO.getMethod()+" "+optionStatement;

        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForFOCE() {

        String methodType = Method.FOCE.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FOCE.getMethod()+" "+EstConstant.FOCE.getStatement();

        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForFOCEI() {

        String methodType = Method.FOCEI.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FOCEI.getMethod()+" "+EstConstant.FOCEI.getStatement();
        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForSAEM() {

        String methodType = Method.SAEM.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.SAEM.getMethod()+" "+EstConstant.SAEM.getStatement();

        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForSAEMWhenCountData() {

        String methodType = Method.SAEM.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.SAEM.getMethod()+" "+EstConstant.SAEM.getStatement()+
                EstConstant.COUNT_DATA_LAPLACE_OPTION.getStatement();

        when(discreteHandler.isCountData()).thenReturn(true);
        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForSAEMWhenTTE() {

        String methodType = Method.SAEM.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.SAEM.getMethod()+" "+EstConstant.SAEM.getStatement()+
                EstConstant.TTE_DATA_LAPLACE_OPTION.getStatement();

        when(discreteHandler.isTimeToEventData()).thenReturn(true);
        verifyStatement(methodType, expectedStatement);
    }

    private void verifyStatement(String methodType, String expectedStatement){
        estStatementBuilder = new EstimationStatementBuilder(discreteHandler, estOptions);

        String generatedStatement = estStatementBuilder.buildEstimationStatementFromAlgorithm(methodType).toString().trim();

        assertNotNull("estimation statement cannot be null", generatedStatement);
        assertFalse("estimation statement is expected here.", generatedStatement.isEmpty());
        assertEquals("correct statement should be generated for "+methodType, expectedStatement, generatedStatement);
    }

}
