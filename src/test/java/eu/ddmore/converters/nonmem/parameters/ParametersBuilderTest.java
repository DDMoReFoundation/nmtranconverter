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
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.utils.OrderedThetasHandler;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import static org.powermock.api.mockito.PowerMockito.when;

public class ParametersBuilderTest extends ParametersMockHelper {

    @Mock PopulationParameter popParameter;
    @Mock OrderedThetasHandler orderedThetasHandler;
    private List<PopulationParameter> populationParameters;
    private ParametersBuilder parametersBuilder;

    private Map<Integer, String> orderedThetas;
    @Before
    public void setUp() throws Exception {
        populationParameters = new ArrayList<>();
        populationParameters.add(popParameter);
        
        orderedThetas = new TreeMap<Integer, String>();
        when(context.getParameterInitialiser()).thenReturn(paramInitialiser);
        when(context.getCorrelationHandler()).thenReturn(correlationHandler);
        when(context.getIovHandler()).thenReturn(iovHandler);

        when(context.getOrderedThetasHandler()).thenReturn(orderedThetasHandler);
        when(orderedThetasHandler.getOrderedThetas()).thenReturn(orderedThetas);
    }

    @Test(expected=IllegalStateException.class)
    public void shouldThrowExceptionIfParamBlocksEmpty(){
        parametersBuilder = new ParametersBuilder(context);
        parametersBuilder.initialiseAllParameters();
    }
}
