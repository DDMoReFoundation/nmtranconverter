/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.ProblemStatement;

public class ProblemStatementTest {

	private static final String STATEMENT_BLOCK_NAME = "$PROBLEM";
	private static final String PROBLEM_DESCRIPTION = "Test Problem Description";


	@Test
	public void shouldCreateValidProblemStatement() {

		ProblemStatement statement = new ProblemStatement(PROBLEM_DESCRIPTION);

		assertNotNull("ProblemStatement should not be null.", statement);
		assertEquals("problemDescription should be correct.", PROBLEM_DESCRIPTION, statement.getProblemDescription());
		assertEquals("ProblemStatement should be correct.",
				STATEMENT_BLOCK_NAME + " " + PROBLEM_DESCRIPTION, statement.getStatement());
	}
	
	@Test
	public void shouldCreateValidProblemStatementWithNullDescription() {

		ProblemStatement statement = new ProblemStatement(null);

		assertNull("problemDescription should be null.", statement.getProblemDescription());
		assertEquals("ProblemStatement should be correct.",
				STATEMENT_BLOCK_NAME, statement.getStatement());
	}
}