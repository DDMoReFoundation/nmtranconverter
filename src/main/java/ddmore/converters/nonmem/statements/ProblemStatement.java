package ddmore.converters.nonmem.statements;

import java.io.IOException;
import java.io.Writer;

public class ProblemStatement implements Writeable {
	
	String problemDescription;
	String statement;
	
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
			StringBuilder stringBuilder = new StringBuilder("$PROBLEM");

			if (problemDescription != null) {
				stringBuilder.append(" " + problemDescription);
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