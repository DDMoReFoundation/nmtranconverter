/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;
import eu.ddmore.libpharmml.util.WrappedList;

@RunWith(PowerMockRunner.class)
public class InputStatementTest {

    @Mock InputColumnsHandler inputColumns;
    @Mock ExternalDataSet extDataSet;
    @Mock DataSet dataSet;

    InputStatement inputStatement;
    
    private static final String COL_ID_1 = ColumnConstant.ID.toString();
    private static final ColumnType COL_TYPE_1 = ColumnType.ID;
    private static final SymbolType COL_VALUE_1 = SymbolType.ID;
    private static final Integer COL_NUM_1 = new Integer(1);
    private static final ColumnDefinition ID = new ColumnDefinition(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1);
    
    private static final String COL_ID_2 = ColumnConstant.TIME.toString();
    private static final ColumnType COL_TYPE_2 = ColumnType.IDV;
    private static final SymbolType COL_VALUE_2 = SymbolType.ID;
    private static final Integer COL_NUM_2 = new Integer(2);
    private static final ColumnDefinition TIME = new ColumnDefinition(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2);
    
    private static final String COL_ID_3 = "WT";
    private static final ColumnType COL_TYPE_3 = ColumnType.UNDEFINED;
    private static final SymbolType COL_VALUE_3 = SymbolType.ID;
    private static final Integer COL_NUM_3 = new Integer(3);
    private static final ColumnDefinition WT = new ColumnDefinition(COL_ID_3, COL_TYPE_3, COL_VALUE_3, COL_NUM_3);
    
    private static final String COL_ID_4 = "AMT";
    private static final ColumnType COL_TYPE_4 = ColumnType.COVARIATE;
    private static final SymbolType COL_VALUE_4 = SymbolType.ID;
    private static final Integer COL_NUM_4 = new Integer(4);
    private static final ColumnDefinition AMT = new ColumnDefinition(COL_ID_4, COL_TYPE_4, COL_VALUE_4, COL_NUM_4);
    
    private static final String COL_ID_5 = "EVID";
    private static final ColumnType COL_TYPE_5 = ColumnType.UNDEFINED;
    private static final SymbolType COL_VALUE_5 = SymbolType.ID;
    private static final Integer COL_NUM_5 = new Integer(5);
    private static final ColumnDefinition EVID = new ColumnDefinition(COL_ID_5, COL_TYPE_5, COL_VALUE_5, COL_NUM_5);

    WrappedList<ColumnDefinition> dataColumns;
    List<ExternalDataSet> externalDataSets = new ArrayList<ExternalDataSet>();

    @Before
    public void setUp() throws Exception {
        dataColumns = new WrappedList<ColumnDefinition>();
        dataColumns.add(ID);
        dataColumns.add(TIME);
        when(dataSet.getListOfColumnDefinition()).thenReturn(dataColumns);

        when(extDataSet.getDataSet()).thenReturn(dataSet);
        externalDataSets.add(extDataSet);
    }

    @Test
    public void shouldReturnInputStatement() {
        dataColumns.add(WT);
        dataColumns.add(AMT);
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets);
        
        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
    }

    @Test
    public void shouldReturnInputStatementWithDrop() {
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets);
        
        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
        assertTrue("should contain DROP for missing columns in order",inputStmt.contains("DROP"));
    }
    
    @Test
    public void shouldMarkColumnDropped() {
        dataColumns.add(WT);
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets);
        
        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
        //if column has columntype "UNDEFINED" then mark it as dropped
        assertTrue("Should have column marked as dropped when columntype is undefined.",inputStmt.contains(WT.getColumnId()+"=DROP"));
        //if column "EVID" has columntype "UNDEFINED" then do not mark it as dropped        
        assertFalse("Column EVID should not be marked as dropped when columntype is undefined.",inputStmt.contains(EVID.getColumnId()+"=DROP"));
    }
    
}