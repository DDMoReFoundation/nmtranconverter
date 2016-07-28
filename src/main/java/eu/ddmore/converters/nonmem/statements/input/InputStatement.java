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
package eu.ddmore.converters.nonmem.statements.input;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;

public class InputStatement {

    private static final String DROP = "DROP";
    private String statement;
    private InputColumnsHandler columnsHandler;

    public InputStatement(InputColumnsHandler columnsHandler) {
        Preconditions.checkNotNull(columnsHandler, "Conversion Context cannot be null");
        this.columnsHandler = columnsHandler;
    }

    /**
     * @return the printable version of this statement
     */
    public String getStatement() {

        if (null == statement) {
            StringBuilder stringBuilder = new StringBuilder(Formatter.input());

            for (InputColumn nextColumn : columnsHandler.getInputColumnsProvider().getInputHeaders()) {
                stringBuilder.append(" " + nextColumn.getColumnId());
                if(nextColumn.isDropped()){
                    stringBuilder.append("="+DROP);
                }

                String newColumnName = Formatter.propertyHandler.getColumnNameForColumnType(nextColumn.getColumnType().toString());

                if(StringUtils.isNotEmpty(newColumnName) 
                        && !Formatter.propertyHandler.isReservedColumnName(nextColumn.getColumnId())){
                    stringBuilder.append("="+newColumnName);
                }

            }
            statement = stringBuilder.toString();
        }

        return statement;
    }
}