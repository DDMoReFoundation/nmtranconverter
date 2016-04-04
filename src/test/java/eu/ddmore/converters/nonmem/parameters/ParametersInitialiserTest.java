/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
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
