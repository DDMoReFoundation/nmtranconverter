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

import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.HeaderColumnsDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;
import eu.ddmore.libpharmml.util.WrappedList;

@RunWith(PowerMockRunner.class)
public class InputStatementTest extends BasicTestSetup {

    @Mock InputColumnsHandler inputColumns;
    @Mock ExternalDataSet extDataSet;
    @Mock HeaderColumnsDefinition headerColumnDef;
    @Mock DataSet dataSet;

    InputStatement inputStatement;

    WrappedList<ColumnDefinition> dataColumns;
    List<ExternalDataSet> externalDataSets = new ArrayList<ExternalDataSet>();
    List<CovariateDefinition> covDefinitions = new ArrayList<CovariateDefinition>();

    @Before
    public void setUp() throws Exception {
        dataColumns = new WrappedList<ColumnDefinition>();
        dataColumns.add(ID);
        dataColumns.add(TIME);
        when(headerColumnDef.getListOfColumn()).thenReturn(dataColumns);
        when(dataSet.getDefinition()).thenReturn(headerColumnDef);

        when(extDataSet.getDataSet()).thenReturn(dataSet);
        externalDataSets.add(extDataSet);
    }

    @Test
    public void shouldReturnInputStatement() {
        dataColumns.add(WT);
        dataColumns.add(AMT);
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets,covDefinitions);

        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
    }

    @Test
    public void shouldReturnInputStatementWithDrop() {
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets, covDefinitions);

        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
        assertTrue("should contain DROP for missing columns in order",inputStmt.contains("DROP"));
    }

    @Test
    public void shouldMarkColumnDropped() {
        dataColumns.add(WT);
        dataColumns.add(EVID);
        inputColumns = new InputColumnsHandler(externalDataSets, covDefinitions);

        inputStatement = new InputStatement(inputColumns);
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
        //if column has columntype "UNDEFINED" then mark it as dropped
        assertTrue("Should have column marked as dropped when columntype is undefined.",inputStmt.contains(WT.getColumnId()+"=DROP"));
        //if column "EVID" has columntype "UNDEFINED" then do not mark it as dropped        
        assertFalse("Column EVID should not be marked as dropped when columntype is undefined.",inputStmt.contains(EVID.getColumnId()+"=DROP"));
    }

}