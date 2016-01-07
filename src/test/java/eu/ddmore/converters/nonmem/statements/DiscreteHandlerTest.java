/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.libpharmml.dom.modeldefn.CountData;
import eu.ddmore.libpharmml.dom.modeldefn.CountPMF;
import eu.ddmore.libpharmml.dom.modeldefn.TTEFunction;
import eu.ddmore.libpharmml.dom.modeldefn.TimeToEventData;
import eu.ddmore.libpharmml.dom.uncertml.NaturalNumberValueType;
import eu.ddmore.libpharmml.dom.uncertml.NegativeBinomialDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;
import eu.ddmore.libpharmml.dom.uncertml.ProbabilityValueType;
import eu.ddmore.libpharmml.dom.uncertml.VarRefType;


/**
 * Testing discrete handler class
 */
@RunWith(PowerMockRunner.class)
public class DiscreteHandlerTest {

    @Mock ConversionContext context;
    @Mock ScriptDefinition definition;
    @Mock ObservationBlock observationBlock;
    @Mock CountData countData;
    @Mock CountPMF countPMF;
    @Mock PoissonDistribution poissonDist;
    @Mock NegativeBinomialDistribution negativeBinomialDist;

    @Mock PositiveRealValueType realValueType;
    @Mock NaturalNumberValueType naturalNumValueType;
    @Mock ProbabilityValueType probabilityValueType;
    @Mock VarRefType var;

    @Mock TimeToEventData tteData;
    @Mock TTEFunction tteFunction;


    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(definition);
        List<ObservationBlock> obsBlocks = new ArrayList<ObservationBlock>();
        obsBlocks.add(observationBlock);

        when(context.getScriptDefinition().getObservationBlocks()).thenReturn(obsBlocks);
        when(observationBlock.isDiscrete()).thenReturn(true);
    }

    private void poissonCountDataSetUp(){
        when(observationBlock.getCountData()).thenReturn(countData);
        List<CountPMF> countPMFs = new ArrayList<CountPMF>();
        countPMFs.add(countPMF);
        when(countData.getListOfPMF()).thenReturn(countPMFs);

        when(countPMF.getDistribution()).thenReturn(poissonDist);

        when(poissonDist.getRate()).thenReturn(realValueType);
        when(realValueType.getVar()).thenReturn(var);
        when(var.getVarId()).thenReturn("LAMBDA");
    }

    private void negativeBinomialSetUp(){
        when(observationBlock.getCountData()).thenReturn(countData);
        List<CountPMF> countPMFs = new ArrayList<CountPMF>();
        countPMFs.add(countPMF);
        when(countData.getListOfPMF()).thenReturn(countPMFs);

        when(countPMF.getDistribution()).thenReturn(negativeBinomialDist);

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

        DiscreteHandler discreteHandler = new DiscreteHandler(definition);
        assertTrue("The discrete data should have count data", discreteHandler.isCountData());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

    @Test
    public void testIfDiscreteStatementsAreCreatedForNegativeBinData() {
        negativeBinomialSetUp();

        DiscreteHandler discreteHandler = new DiscreteHandler(definition);
        assertTrue("The discrete data should have negative binomial", discreteHandler.isNegativeBinomial());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

    @Test
    public void testIfDiscreteStatementsAreCreatedForTimeToEventData() {
        timeToEventDataSetUp();

        DiscreteHandler discreteHandler = new DiscreteHandler(definition);
        assertTrue("The discrete data should have time to event data", discreteHandler.isTimeToEventData());
        assertFalse(discreteHandler.getDiscreteStatement().toString().isEmpty());
    }

}
