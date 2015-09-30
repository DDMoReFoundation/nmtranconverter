/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
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
        Preconditions.checkNotNull(dataSetHandler, "conversion context cannot be null");
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