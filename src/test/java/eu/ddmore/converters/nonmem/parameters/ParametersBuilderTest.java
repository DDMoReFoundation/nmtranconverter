/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.utils.OrderedThetasHandler;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import static org.powermock.api.mockito.PowerMockito.when;

public class ParametersBuilderTest extends ParametersMockHelper {

    @Mock PopulationParameter popParameter;
    @Mock OrderedThetasHandler orderedThetasHandler;
    List<PopulationParameter> populationParameters;
    ParametersBuilder parametersBuilder;

    Map<Integer, String> orderedThetas;
    @Before
    public void setUp() throws Exception {
        populationParameters = new ArrayList<>();
        populationParameters.add(popParameter);
        
        orderedThetas = new HashMap<Integer, String>();
        when(context.getParameterInitialiser()).thenReturn(paramInitialiser);
        when(context.getCorrelationHandler()).thenReturn(correlationHandler);
        when(context.getIovHandler()).thenReturn(iovHandler);

        when(context.getOrderedThetasHandler()).thenReturn(orderedThetasHandler);
        when(orderedThetasHandler.getOrderedThetas()).thenReturn(orderedThetas);
    }

    @Test(expected=IllegalStateException.class)
    public void shouldThrowExceptionIfParamBlocksEmpty(){
        parametersBuilder = new ParametersBuilder(context);
        parametersBuilder.initialiseAllParameters(populationParameters);
    }
}
