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
package eu.ddmore.converters.nonmem.handlers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;

public class PropertiesHandlerTest extends BasicTestSetup {

    private PropertiesHandler propertiesHandler;

    @Test
    public void shouldGetColumnNameForColumnType() {
        final String cmtColumn = "ADM"; //CMT
        final String cmtColumnSymbol = "CMT";

        propertiesHandler = new PropertiesHandler();
        String resultedColumn = propertiesHandler.getColumnNameForColumnType(cmtColumn);
        assertNotNull("column name for column type should not be null.", resultedColumn);
        assertTrue("Should get column name for column type.", resultedColumn.equals(cmtColumnSymbol));
    }

    @Test
    public void shouldGetReservedColumnNameForColumn() {
        final String cmtColumn = "CMT"; //CMT

        propertiesHandler = new PropertiesHandler();
        boolean resultedColumn = propertiesHandler.isReservedColumnName(cmtColumn);
        assertNotNull("column name for column type should not be null.", resultedColumn);
        assertTrue("Should get a reserved name.", resultedColumn);
    }

    @Test
    public void shouldGetBinopPropertyFor() {
        final String plusSymbol = "+";
        final String plus = "plus"; //+
        propertiesHandler = new PropertiesHandler();
        String resultSymbol = propertiesHandler.getBinopPropertyFor(plus);
        assertNotNull("column name for column type should not be null.", resultSymbol);
        assertTrue("Should get symbol for binop property.", resultSymbol.equals(plusSymbol));
    }

    @Test
    public void shouldGetReservedWordFor() {
        final String pred = "PRED";
        final String predSymbol = "NM_PRED"; //NM_PRED

        propertiesHandler = new PropertiesHandler();
        String resultSymbol = propertiesHandler.getReservedWordFor(pred);
        assertNotNull("column name for column type should not be null.", resultSymbol);
        assertTrue("Should get symbol for reserved word.", resultSymbol.equals(predSymbol));
    }

}
