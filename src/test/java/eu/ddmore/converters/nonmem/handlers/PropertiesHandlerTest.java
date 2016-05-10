package eu.ddmore.converters.nonmem.handlers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;

public class PropertiesHandlerTest extends BasicTestSetup {

    private PropertiesHandler propertiesHandler;

    @Test
    public void shouldGetColumnNameForColumnType() {
        final String cmtColumn = "CMT"; //CMT

        propertiesHandler = new PropertiesHandler();
        String resultedColumn = propertiesHandler.getColumnNameForColumnType(cmtColumn);
        assertNotNull("column name for column type should not be null.", resultedColumn);
        assertTrue("Should get column name for column type.", resultedColumn.equals(cmtColumn));
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
