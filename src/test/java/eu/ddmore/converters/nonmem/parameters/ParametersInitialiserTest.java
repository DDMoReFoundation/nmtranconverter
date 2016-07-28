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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(ScriptDefinitionAccessor.class)
public class ParametersInitialiserTest extends BasicTestSetup {

    @Mock PopulationParameter populationParameter;
    private ParametersInitialiser parametersInitialiser;
    private List<PopulationParameter> populationParameters;

    @Before
    public void setUp() throws Exception {
        mockStatic(ScriptDefinitionAccessor.class);
        populationParameters = new ArrayList<>();
        populationParameters.add(populationParameter);
        
        when(ScriptDefinitionAccessor.getEstimationStep(scriptDefinition)).thenReturn(estStep);
    }

    @Test
    public void shouldPopulateParameterInitialiser() {
        parametersInitialiser = new ParametersInitialiser(populationParameters, scriptDefinition);
        assertNotNull("Should get parameters to estimate", parametersInitialiser.getParametersToEstimate());
    }

}
