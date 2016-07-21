/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
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