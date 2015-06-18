/**
 * 
 */
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

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
import eu.ddmore.libpharmml.dom.modeldefn.CountData;
import eu.ddmore.libpharmml.dom.modeldefn.CountPMF;
import eu.ddmore.libpharmml.dom.modeldefn.TTEFunction;
import eu.ddmore.libpharmml.dom.modeldefn.TimeToEventData;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;
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
    @Mock PositiveRealValueType realValueType;
    @Mock VarRefType var;
    
    @Mock TimeToEventData tteData;
    @Mock TTEFunction tteFunction;
    
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(definition);
        List<ObservationBlock> obsBlocks = new ArrayList<ObservationBlock>();
        obsBlocks.add(observationBlock);
        
        when(context.getScriptDefinition().getObservationBlocks()).thenReturn(obsBlocks);
        when(observationBlock.isDiscrete()).thenReturn(true);
    }
    
    private void CountDataSetUp(){
        when(observationBlock.getCountData()).thenReturn(countData);
        List<CountPMF> countPMFs = new ArrayList<CountPMF>();
        countPMFs.add(countPMF);
        when(countData.getListOfPMF()).thenReturn(countPMFs);
        
        when(countPMF.getDistribution()).thenReturn(poissonDist);
        
        when(poissonDist.getRate()).thenReturn(realValueType);
        when(poissonDist.getRate().getVar()).thenReturn(var);
        when(poissonDist.getRate().getVar().getVarId()).thenReturn("LAMBDA");
    }
    
    private void TimeToEventDataSetUp(){
        when(observationBlock.getTimeToEventData()).thenReturn(tteData);
        
        List<TTEFunction> tteFunctions = new ArrayList<TTEFunction>();
        tteFunctions.add(tteFunction);
        when(tteData.getListOfHazardFunction()).thenReturn(tteFunctions);
        
        when(tteFunction.getSymbId()).thenReturn("HAZ");
    }
    
    /**
     * Checks if discrete statements are created for count data when it exists.
     */
    @Test
    public void testIfDiscreteStatementsAreCreatedForCountData() {
        CountDataSetUp();
        
        DiscreteHandler discreteHandler = new DiscreteHandler();
        assertFalse(discreteHandler.getDiscreteDetails(context).toString().isEmpty());
    }
    
    /**
     * Checks if discrete statements are created for time to event data when it exists.
     */
    @Test
    public void testIfDiscreteStatementsAreCreatedForTimeToEventData() {
        TimeToEventDataSetUp();
        
        DiscreteHandler discreteHandler = new DiscreteHandler();
        assertFalse(discreteHandler.getDiscreteDetails(context).toString().isEmpty());
    }

}