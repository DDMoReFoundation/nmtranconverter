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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class ThetaStatementBuilderTest extends ParametersMockHelper {

    @Mock ParametersBuilder parametersBuilder;

    @Before
    public void setUp() throws Exception {
        parametersBuilder = Mockito.mock(ParametersBuilder.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void shouldGetThetaStatementBlock() {
        ThetaStatementBuilder thetaStatementBuilder = new ThetaStatementBuilder(parametersBuilder, correlationHandler, paramInitialiser);
        assertNotNull("Should get theta statements block.", thetaStatementBuilder.getThetaStatementBlock());
    }

}
