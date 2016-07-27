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

import com.google.common.base.Preconditions;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * This class creates data statement block for nmtran
 */
public class DataStatement{

    private String statement;
    private final DataSetHandler dataSetHandler;

    public DataStatement(DataSetHandler dataSetHandler) {
        Preconditions.checkNotNull(dataSetHandler, "data set handler cannot be null");
        this.dataSetHandler = dataSetHandler;
    }

    /**
     * This method returns the data statement.
     * The data file name is retrieved from nonmem dataset 
     * 
     * @return the printable version of this statement
     */
    public String getStatement() {

        StringBuilder stringBuilder = new StringBuilder(Formatter.data());
        stringBuilder.append("\"" + dataSetHandler.getDataFileName() + "\"");
        stringBuilder.append(" IGNORE="+dataSetHandler.getIgnoreChar());
        statement = stringBuilder.toString();
        return statement;
    }
}