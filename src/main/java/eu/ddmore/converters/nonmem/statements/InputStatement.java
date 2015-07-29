/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ReservedColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

public class InputStatement {

    private static final String DROP = "DROP";
    private InputColumnsProvider inputColumns = new InputColumnsProvider();
    private String statement;
    private ConversionContext context;

    public InputStatement(ConversionContext convContext) {
        Preconditions.checkNotNull(convContext, "Conversion Context cannot be null");
        context = convContext;
    }

    /**
     * Computes estimation headers for external datasets retrieved.
     * 
     * @param dataFiles
     */
    private void getHeadersforExternalDataSets(List<ExternalDataSet> dataFiles) {

        Preconditions.checkNotNull(dataFiles, "External data set(s) cannot be null");

        for(ExternalDataSet dataFile : dataFiles) {
            getEstimationHeaders(dataFile);
        }
    }

    private void getEstimationHeaders(ExternalDataSet externalDataSet) {

        List<ColumnDefinition> dataColumns = externalDataSet.getDataSet().getListOfColumnDefinition();
        Preconditions.checkNotNull(dataColumns, "External data set has no columns");

        Map<Long, InputHeader> orderedColumns = new TreeMap<Long, InputHeader>();
        Long columnSequence= new Long(1);

        for (ColumnDefinition dataColumn : dataColumns) {
            columnSequence = addToOrderedColumns(dataColumn, columnSequence, orderedColumns);
        }
        
        inputColumns.getInputHeaders().addAll(orderedColumns.values());
    }

    private Long addToOrderedColumns(ColumnDefinition dataColumn, Long columnSequence, Map<Long, InputHeader> orderedColumns) {
        String columnId = dataColumn.getColumnId().toUpperCase();
        Long columnNumber = dataColumn.getColumnNum().longValue();

        if(columnNumber>columnSequence){
            for(;columnSequence<columnNumber;columnSequence++){
                if(!orderedColumns.containsKey(columnNumber)){
                    InputHeader emptyColumn = new InputHeader(DROP, false, columnNumber);
                    orderedColumns.put(columnSequence, emptyColumn);
                }
            }
        }

        if (orderedColumns.containsKey(columnNumber) && !orderedColumns.get(columnNumber).getColumnId().equals(DROP) ) {
            throw new IllegalStateException("External data set contains duplicate columns for : "+columnId);
        } else {
            ColumnType columnType = dataColumn.getColumnType();
            Boolean isDropped = (columnType.equals(ColumnType.UNDEFINED) && !ReservedColumnConstant.contains(columnId));

            InputHeader inputHeader = new InputHeader(columnId, isDropped, columnNumber);
            orderedColumns.put(columnNumber, inputHeader);
            
            populateCovTableDetails(columnType,dataColumn.getValueType(),columnId);
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
                inputColumns.getCatCovTableColumns().add(symbol.toUpperCase());	
            }
            if(valueType.equals(SymbolType.REAL)){
                inputColumns.getContCovTableColumns().add(symbol.toUpperCase());
            }
        }
    }

    /**
     * @return the printable version of this statement
     */
    public String getStatement() {
        
        List<ExternalDataSet> dataFiles = context.retrieveExternalDataSets();
        if(dataFiles!=null && !dataFiles.isEmpty()){
            getHeadersforExternalDataSets(dataFiles);
        }else{
            throw new IllegalArgumentException("data file should be present to get input headers");
        }
        
        context.setInputColumnsProvider(inputColumns);
        
        if (null == statement) {
            StringBuilder stringBuilder = new StringBuilder(Formatter.input());

            for (InputHeader nextColumn : inputColumns.getInputHeaders()) {
                stringBuilder.append(" " + nextColumn.getColumnId());
                if(nextColumn.isDropped()){
                    stringBuilder.append("="+DROP);
                }
            }
            statement = stringBuilder.toString();
        }

        return statement;
    }
}