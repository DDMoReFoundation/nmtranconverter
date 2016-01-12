/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.powermock.api.mockito.PowerMockito.when;

import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DataStatementTest extends BasicTestSetup {

    @Mock DataSetHandler dataSetHandler;

    DataStatement dataStatement;

    private final String dataFileName = "datafile.csv";
    private static final Character IGNORE_CHAR = '@';

    private final String expectedDataStatement = "$DATA \""+dataFileName+"\" IGNORE="+IGNORE_CHAR;

    @Test
    public void shouldGetDataStatement(){
        dataStatement = new DataStatement(dataSetHandler);

        when(dataSetHandler.getDataFileName()).thenReturn(dataFileName);
        when(dataSetHandler.getIgnoreChar()).thenReturn(IGNORE_CHAR);

        String outputDataStatement = dataStatement.getStatement();

        assertEquals("Data statement should be created in expected format.", expectedDataStatement, outputDataStatement);

    }

}