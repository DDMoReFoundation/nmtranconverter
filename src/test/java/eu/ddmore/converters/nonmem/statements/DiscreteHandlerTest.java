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
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import crx.converter.spi.blocks.ObservationBlock;

import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.libpharmml.dom.modeldefn.CountData;
import eu.ddmore.libpharmml.dom.modeldefn.CountPMF;
import eu.ddmore.libpharmml.dom.modeldefn.TTEFunction;
import eu.ddmore.libpharmml.dom.modeldefn.TimeToEventData;
import eu.ddmore.libpharmml.dom.modeldefn.UncertML;
import eu.ddmore.libpharmml.dom.uncertml.NaturalNumberValueType;
import eu.ddmore.libpharmml.dom.uncertml.NegativeBinomialDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;
import eu.ddmore.libpharmml.dom.uncertml.ProbabilityValueType;
import eu.ddmore.libpharmml.dom.uncertml.VarRefType;


/**
 * Testing discrete handler class
 */
public class DiscreteHandlerTest extends BasicTestSetup {

    @Mock ObservationBlock observationBlock;
    @Mock CountData countData;

    CountPMF countPMF;
    @Mock UncertML uncertML;
    @Mock PoissonDistribution poissonDist;
    @Mock NegativeBinomialDistribution negativeBinomialDist;
    @Mock JAXBElement<?> abstractDiscreteUnivariateDist;

    @Mock PositiveRealValueType realValueType;
    @Mock NaturalNumberValueType naturalNumValueType;
    @Mock ProbabilityValueType probabilityValueType;
    @Mock VarRefType var;

    @Mock TimeToEventData tteData;
    @Mock TTEFunction tteFunction;

    @Before
    public void setUp() throws Exception {
        List<ObservationBlock> obsBlocks = new ArrayList<ObservationBlock>();
        obsBlocks.add(observationBlock);

        when(context.getScriptDefinition().getObservationBlocks()).thenReturn(obsBlocks);
        when(observationBlock.isDiscrete()).thenReturn(true);

        countPMF = mock(CountPMF.class,RETURNS_DEEP_STUBS);
        doReturn(abstractDiscreteUnivariateDist).when(uncertML).getAbstractDiscreteUnivariateDistribution();
        when(countPMF.getDistribution().getUncertML()).thenReturn(uncertML);

    }

    private void poissonCountDataSetUp(){
        doReturn(poissonDist).when(abstractDiscreteUnivariateDist).getValue();

        when(observationBlock.getCountData()).thenReturn(countData);
        List<CountPMF> countPMFs = new ArrayList<CountPMF>();
        countPMFs.add(countPMF);
        when(countData.getListOfPMF()).thenReturn(countPMFs);

        when(poissonDist.getRate()).thenReturn(realValueType);
        when(realValueType.getVar()).thenReturn(var);
        when(var.getVarId()).thenReturn("LAMBDA");
    }

    private void negativeBinomialSetUp(){
        doReturn(negativeBinomialDist).when(abstractDiscreteUnivariateDist).getValue();

        when(observationBlock.getCountData()).thenReturn(countData);
        List<CountPMF> countPMFs = new ArrayList<CountPMF>();
        countPMFs.add(countPMF);
        when(countData.getListOfPMF()).thenReturn(countPMFs);

        when(negativeBinomialDist.getNumberOfFailures()).thenReturn(naturalNumValueType);
        when(naturalNumValueType.getVar()).thenReturn(var);
        when(var.getVarId()).thenReturn("NFL");
        when(naturalNumValueType.getNVal()).thenReturn(new BigInteger("1"));

        when(negativeBinomialDist.getProbability()).thenReturn(probabilityValueType);
        when(probabilityValueType.getVar()).thenReturn(var);
        when(var.getVarId()).thenReturn("PROB");
        when(probabilityValueType.getPVal()).thenReturn(new Double("1"));
    }

    private void timeToEventDataSetUp(){
        when(observationBlock.getTimeToEventData()).thenReturn(tteData);

        List<TTEFunction> tteFunctions = new ArrayList<TTEFunction>();
        tteFunctions.add(tteFunction);
        when(tteData.getListOfHazardFunction()).thenReturn(tteFunctions);

        when(tteFunction.getSymbId()).thenReturn("HAZ");
    }

    @Test
    public void testIfDiscreteStatementsAreCreatedForCountData() {
        poissonCountDataSetUp();

        DiscreteHandler discreteHandler = new DiscreteHandler(scriptDefinition);
        assertTrue("The discrete data should have count data", discreteHandler.isCountData());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

    @Test
    public void testIfDiscreteStatementsAreCreatedForNegativeBinData() {
        negativeBinomialSetUp();

        DiscreteHandler discreteHandler = new DiscreteHandler(scriptDefinition);
        assertTrue("The discrete data should have negative binomial", discreteHandler.isNegativeBinomial());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

    @Test
    public void testIfDiscreteStatementsAreCreatedForTimeToEventData() {
        timeToEventDataSetUp();

        DiscreteHandler discreteHandler = new DiscreteHandler(scriptDefinition);
        assertTrue("The discrete data should have time to event data", discreteHandler.isTimeToEventData());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

}
