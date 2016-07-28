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
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.ProblemStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;

public class ProblemStatementTest {

    private static final String PROBLEM_DESCRIPTION = "Test Problem Description";


    @Test
    public void shouldCreateValidProblemStatement() {

        ProblemStatement statement = new ProblemStatement(PROBLEM_DESCRIPTION);

        assertNotNull("ProblemStatement should not be null.", statement);
        assertEquals("problemDescription should be correct.", PROBLEM_DESCRIPTION, statement.getProblemDescription());
        assertEquals("ProblemStatement should be correct.",
            Formatter.problem() + PROBLEM_DESCRIPTION, statement.getStatement());
    }

    @Test
    public void shouldCreateValidProblemStatementWithNullDescription() {

        ProblemStatement statement = new ProblemStatement(null);

        assertNull("problemDescription should be null.", statement.getProblemDescription());
        assertEquals("ProblemStatement should be correct.",
            Formatter.problem(), statement.getStatement());
    }
}