/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ReservedColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

/**
 * This class handles input columns operations and facilitates input columns provider for access to columns.
 */
public class InputColumnsHandler {

    private static final String DROP = "DROP";
    private InputColumnsProvider inputColumnsProvider = new InputColumnsProvider();

    public InputColumnsHandler(final List<ExternalDataSet> dataSets) {
        Preconditions.checkNotNull(dataSets, "conversion context cannot be null");

        if(!dataSets.isEmpty()){
            populateColumnsWithHeadersforDataSets(dataSets);
        }else{
            throw new IllegalArgumentException("data file should be present to get input headers");
        }
    }

    /**
     * Computes estimation headers for external datasets retrieved.
     * 
     * @param dataFiles
     */
    private void populateColumnsWithHeadersforDataSets(List<ExternalDataSet> dataFiles) {

        for(ExternalDataSet dataFile : dataFiles) {
            populateColumnsWithEstHeaders(dataFile);
        }
    }

    private void populateColumnsWithEstHeaders(ExternalDataSet externalDataSet) {

        List<ColumnDefinition> dataColumns = externalDataSet.getDataSet().getListOfColumnDefinition();
        Preconditions.checkNotNull(dataColumns, "External data set has no columns");

        Map<Long, InputHeader> orderedColumns = new TreeMap<Long, InputHeader>();
        Long columnSequence= new Long(1);

        for (ColumnDefinition dataColumn : dataColumns) {
            columnSequence = addToOrderedColumns(dataColumn, columnSequence, orderedColumns);
        }

        inputColumnsProvider.getInputHeaders().addAll(orderedColumns.values());
    }

    private Long addToOrderedColumns(ColumnDefinition dataColumn, Long columnSequence, Map<Long, InputHeader> orderedColumns) {
        String columnId = dataColumn.getColumnId().toUpperCase();
        Long columnNumber = dataColumn.getColumnNum().longValue();

        if(columnNumber>columnSequence){
            for(;columnSequence<columnNumber;columnSequence++){
                if(!orderedColumns.containsKey(columnNumber)){
                    InputHeader emptyColumn = new InputHeader(DROP, false, columnNumber, dataColumn.getColumnType());
                    orderedColumns.put(columnSequence, emptyColumn);
                }
            }
        }

        if (orderedColumns.containsKey(columnNumber) && !orderedColumns.get(columnNumber).getColumnId().equals(DROP) ) {
            throw new IllegalStateException("External data set contains duplicate columns for : "+columnId);
        } else {
            ColumnType columnType = dataColumn.getColumnType();
            Boolean isDropped = (columnType.equals(ColumnType.UNDEFINED) && !ReservedColumnConstant.contains(columnId));

            String formattedColumnId =Formatter.getReservedParam(columnId);
            InputHeader inputHeader = new InputHeader(formattedColumnId, isDropped, columnNumber, columnType);
            orderedColumns.put(columnNumber, inputHeader);

            populateCovTableDetails(columnType,dataColumn.getValueType(),formattedColumnId);
            columnSequence++;
        }
        return columnSequence;
    }

    /**
     * This method populates cov table column details and creates lists of categorical cov tables and continuous cov tables.
     * These lists are used while creating table statements.
     * @param columnType
     * @param valueType
     * @param symbol
     */
    private void populateCovTableDetails(ColumnType columnType, SymbolType valueType, String symbol){
        if(columnType.equals(ColumnType.COVARIATE)){
            if(valueType.equals(SymbolType.INT)){
                inputColumnsProvider.getCatCovTableColumns().add(symbol.toUpperCase()); 
            }
            if(valueType.equals(SymbolType.REAL)){
                inputColumnsProvider.getContCovTableColumns().add(symbol.toUpperCase());
            }
        }
    }

    public InputColumnsProvider getInputColumnsProvider() {
        return inputColumnsProvider;
    }

    public void setInputColumnsProvider(InputColumnsProvider inputColumnsProvider) {
        this.inputColumnsProvider = inputColumnsProvider;
    }

}
