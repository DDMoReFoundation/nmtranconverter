/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.IOException;
import com.google.common.base.Preconditions;
import eu.ddmore.converters.nonmem.utils.Formatter;

public class DataStatement{

    private String statement;
    private final DataSetHandler dataSetHandler;


    public DataStatement(DataSetHandler dataSetHandler) {
        Preconditions.checkNotNull(dataSetHandler, "conversion context cannot be null");
        this.dataSetHandler = dataSetHandler;
    }

    /**
     * This method will return the data statement.
     * The data file name is retrieved from nonmem dataset 
     * and ignore character is determined with help of first character of data file.
     * 
     * @return the printable version of this statement
     * @throws IOException 
     */
    public String getStatement() throws IOException {

        StringBuilder stringBuilder = new StringBuilder(Formatter.data());
        stringBuilder.append("\"" + dataSetHandler.getDataFileName() + "\"");
        stringBuilder.append(" IGNORE="+dataSetHandler.getIgnoreChar());
        statement = stringBuilder.toString();
        return statement;
    }
}