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