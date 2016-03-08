/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class ThetaStatementBuilderTest extends ParametersMockHelper {

    @Mock ParametersBuilder parametersBuilder;

    ThetaStatementBuilder thetaStatementBuilder;
    @Before
    public void setUp() throws Exception {
        parametersBuilder = Mockito.mock(ParametersBuilder.class, RETURNS_DEEP_STUBS);

    }

    @Test
    public void shouldGetThetaStatementBlock() {
        thetaStatementBuilder = new ThetaStatementBuilder(parametersBuilder, correlationHandler, paramInitialiser);
        assertNotNull("Should get theta statements block.", thetaStatementBuilder.getThetaStatementBlock());
    }

}
