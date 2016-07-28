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

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Test;
import org.mockito.Mock;

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