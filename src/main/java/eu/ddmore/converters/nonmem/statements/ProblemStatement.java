/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.IOException;
import java.io.Writer;

import eu.ddmore.converters.nonmem.utils.Formatter;

public class ProblemStatement implements Writeable {
	
    private String problemDescription;
    private String statement;
	
	public String getProblemDescription() {
		return problemDescription;
	}

	public ProblemStatement(String problemDescription) {
		this.problemDescription = problemDescription;
	}

	/**
	 * @return the printable version of this statement
	 */
	public String getStatement() {

		if (null == statement) {
			StringBuilder stringBuilder = new StringBuilder(Formatter.problem());

			if (problemDescription != null) {
				stringBuilder.append(problemDescription);
			}

			statement = stringBuilder.toString();
		}

		return statement;
	}

	/**
	 * Writes this statement to the given Writer
	 */
	@Override
	public void write(Writer writer) throws IOException {
		writer.write(getStatement());
	}
}