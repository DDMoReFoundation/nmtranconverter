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
