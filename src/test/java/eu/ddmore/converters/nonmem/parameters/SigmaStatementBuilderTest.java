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

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import eu.ddmore.converters.nonmem.utils.Formatter;

public class SigmaStatementBuilderTest extends ParametersMockHelper {

    private final String expectedsigmaStatement = Formatter.endline()+Formatter.sigma()+Formatter.endline("1.0 FIX"); 
    @Before
    public void setUp() throws Exception {
        mockRandomVarHelper();
        epsilonVars = new HashSet<>();
        epsilonVars.add(firstPrv);
    }

    @Test
    public void shouldGetSigmaStatementBlock() {
        SigmaStatementBuilder sigmaStatementBuilder = new SigmaStatementBuilder(context, epsilonVars);
        assertNotNull("Should get sigma statements block", sigmaStatementBuilder.getSigmaStatementBlock());
        assertEquals("Should get expected sigma statement.", expectedsigmaStatement, sigmaStatementBuilder.getSigmaStatementBlock().toString());
    }
}
