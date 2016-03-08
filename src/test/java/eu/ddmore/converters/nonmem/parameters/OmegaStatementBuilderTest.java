/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import org.junit.Before;
import org.junit.Test;
import static org.powermock.api.mockito.PowerMockito.when;

public class OmegaStatementBuilderTest extends ParametersMockHelper {

    OmegaStatementBuilder omegaStatementBuilder;

    @Before
    public void setUp() throws Exception {
        when(context.getParameterInitialiser()).thenReturn(paramInitialiser);
        when(context.getCorrelationHandler()).thenReturn(correlationHandler);
        when(context.getIovHandler()).thenReturn(iovHandler);
    }

    @Test(expected=IllegalStateException.class)
    public void shouldThrowExceptionIfParamBlocksEmpty(){
        omegaStatementBuilder = new OmegaStatementBuilder(context, epsilonVars);
        omegaStatementBuilder.getOmegaStatementBlock();

    }
}
