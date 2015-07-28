/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;
import eu.ddmore.libpharmml.util.WrappedList;

@RunWith(PowerMockRunner.class)
public class InputStatementTest {
    
    @Mock(answer=Answers.RETURNS_DEEP_STUBS) ConversionContext context;
    @Mock ExternalDataSet dataSet;
    
    InputStatement inputStatement;
    
    
    private static final String COL_ID_1 = ColumnConstant.ID.toString();
    private static final ColumnType COL_TYPE_1 = ColumnType.ID;
    private static final SymbolType COL_VALUE_1 = SymbolType.ID;
    private static final Integer COL_NUM_1 = new Integer(1);
    ColumnDefinition id = new ColumnDefinition(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1);
    
    private static final String COL_ID_2 = ColumnConstant.TIME.toString();
    private static final ColumnType COL_TYPE_2 = ColumnType.IDV;
    private static final SymbolType COL_VALUE_2 = SymbolType.ID;
    private static final Integer COL_NUM_2 = new Integer(2);
    ColumnDefinition time = new ColumnDefinition(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2);
    
    private static final String COL_ID_3 = "WT";
    private static final ColumnType COL_TYPE_3 = ColumnType.COVARIATE;
    private static final SymbolType COL_VALUE_3 = SymbolType.ID;
    private static final Integer COL_NUM_3 = new Integer(3);
    ColumnDefinition wt = new ColumnDefinition(COL_ID_3, COL_TYPE_3, COL_VALUE_3, COL_NUM_3);
    
    @Before
    public void setUp() throws Exception {
        inputStatement = new InputStatement(context);
        List<ExternalDataSet> externalDataSets = new ArrayList<ExternalDataSet>();
        when(context.retrieveExternalDataSets()).thenReturn(externalDataSets);
        
        when(externalDataSets.isEmpty()).thenReturn(true);
        WrappedList<ColumnDefinition> dataColumns = new WrappedList<ColumnDefinition>();
        dataColumns.add(id);
        dataColumns.add(time);
        dataColumns.add(wt);
        externalDataSets.add(dataSet);
        
        when(dataSet.getDataSet().getListOfColumnDefinition()).thenReturn(dataColumns);
    }
    
    @Test
    public void shouldReturnInputStatement() {
        String inputStmt = inputStatement.getStatement();
        assertNotNull("should return input statement",inputStmt);
        
    }
    
}