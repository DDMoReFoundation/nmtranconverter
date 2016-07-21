/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.input.InputColumn;
import eu.ddmore.converters.nonmem.statements.input.InputColumnsHandler;
import eu.ddmore.converters.nonmem.statements.input.InputColumnsProvider;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.HeaderColumnsDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;

public class InputColumnsHandlerTest extends BasicTestSetup {

    @Mock ExternalDataSet externalDataSet;
    @Mock DataSet dataSet;
    @Mock HeaderColumnsDefinition definition;
    @Mock ColumnDefinition dataColumn;
    @Mock ColumnDefinition lastDataColumn;

    InputColumnsHandler columnsHandler;

    List<ExternalDataSet> dataSets = new ArrayList<>();
    List<CovariateDefinition> covDefinitions = new ArrayList<>();
    List<ColumnDefinition> dataColumns = new ArrayList<>();
    List<ColumnMapping> columnMappings = new ArrayList<>();

    //should get added in between first column (1) and last column (5)
    InputColumn expectedDropColumn = new InputColumn(DROP, false, COL_NUM_1.intValue()+1, ColumnType.UNDEFINED);

    @Before
    public void setUp() throws Exception {

        dataColumns.add(ID);
        dataColumns.add(AMT);
        dataColumns.add(EVID);

        columnMappings.add(ID_colMapping);
        columnMappings.add(EVID_colMapping);
        columnMappings.add(AMT_colMapping);

        when(definition.getListOfColumn()).thenReturn(dataColumns);
        when(dataSet.getDefinition()).thenReturn(definition);

        when(externalDataSet.getDataSet()).thenReturn(dataSet);
        when(externalDataSet.getListOfColumnMapping()).thenReturn(columnMappings);
        dataSets.add(externalDataSet);
    }

    @Test(expected=IllegalStateException.class)
    public void shouldThrowExceptionForDuplicateColumn() {
        String COL_ID_5 = "DUP_EVID";
        ColumnType COL_TYPE_5 = ColumnType.UNDEFINED;
        SymbolType COL_VALUE_5 = SymbolType.ID;
        Integer COL_NUM_5 = new Integer(5);
        ColumnDefinition DUP_EVID = new ColumnDefinition(COL_ID_5,COL_VALUE_5,COL_NUM_5, COL_TYPE_5);

        dataColumns.add(DUP_EVID);
        columnsHandler = new InputColumnsHandler(dataSets, covDefinitions);
    }

    @Test
    public void shouldGetColumnsProvider() {
        columnsHandler = new InputColumnsHandler(dataSets, covDefinitions);
        InputColumnsProvider columnsProvider = columnsHandler.getInputColumnsProvider();

        assertNotNull("Should return columns provider details", columnsProvider);

        for(InputColumn column : columnsProvider.getInputHeaders()){

            switch(column.getColumnSequence()){
            case 1: //verify if first column is added
                verifyColumn(ID, column);
                break;
            case 2: // expected DROP column
            case 3: // expected DROP column
                assertEquals("Should have expected column name ", expectedDropColumn.getColumnId(), column.getColumnId());
                assertEquals("Should have expected column type", expectedDropColumn.getColumnType(), column.getColumnType());
                break;
            case 4: 
                verifyColumn(AMT, column);
                break;
            case 5:
                verifyColumn(EVID, column);
                break;
            default : fail("unknown and unexpected column type is encountered"); 
            }
        }
    }

    private void verifyColumn(ColumnDefinition expected, InputColumn actual){
        assertEquals("Should have expected column name ", expected.getColumnId(), actual.getColumnId());
        assertEquals("Should have expected column type", expected.getListOfColumnType().get(0), actual.getColumnType());
    }
}
