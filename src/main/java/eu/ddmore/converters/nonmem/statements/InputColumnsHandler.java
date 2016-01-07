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
import eu.ddmore.libpharmml.dom.dataset.HeaderColumnsDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

/**
 * This class handles input columns operations and facilitates input columns provider for access to columns.
 */
public class InputColumnsHandler {

    private static final String DROP = "DROP";
    private InputColumnsProvider inputColumnsProvider = new InputColumnsProvider();

    public InputColumnsHandler(final List<ExternalDataSet> dataSets, List<CovariateDefinition> covDefinitions) {
        Preconditions.checkNotNull(dataSets, "Datasets cannot be null");
        Preconditions.checkArgument(!dataSets.isEmpty(), "data file should be present to get input headers");

        populateColumnsWithHeadersforDataSets(dataSets);
        //These are optional to have and will be added only if they are available.
        if(covDefinitions!=null && !covDefinitions.isEmpty()){
            populateColumnsFromTransformedCov(covDefinitions);
        }
    }

    private void populateColumnsFromTransformedCov(List<CovariateDefinition> covDefinitions) {

        for(CovariateDefinition covDef : covDefinitions){
            if(covDef.getContinuous()!=null){
                ContinuousCovariate contCov = covDef.getContinuous();
                for(CovariateTransformation transformation : contCov.getListOfTransformation()){
                    String transCovId = transformation.getTransformedCovariate().getSymbId();

                    if(transCovId!=null && !transCovId.isEmpty()){
                        inputColumnsProvider.addContCovTableColumn(transCovId);
                    }
                }
            }
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

        HeaderColumnsDefinition headerColumns = externalDataSet.getDataSet().getDefinition();
        Preconditions.checkNotNull(headerColumns, "Header columns definition has no columns");

        List<ColumnDefinition> dataColumns = headerColumns.getListOfColumn();

        Map<Integer, InputColumn> orderedColumns = new TreeMap<Integer, InputColumn>();
        Integer columnSequence= new Integer(1);

        for (ColumnDefinition dataColumn : dataColumns) {
            columnSequence = addToOrderedColumns(dataColumn, columnSequence, orderedColumns);
        }

        inputColumnsProvider.getInputHeaders().addAll(orderedColumns.values());
    }

    private Integer addToOrderedColumns(ColumnDefinition dataColumn, Integer columnSequence, Map<Integer, InputColumn> orderedColumns) {
        String columnId = dataColumn.getColumnId().toUpperCase();
        Integer columnNumber = dataColumn.getColumnNum().intValue();

        if(columnNumber>columnSequence){
            for(;columnSequence<columnNumber;columnSequence++){
                if(!orderedColumns.containsKey(columnNumber)){
                    InputColumn emptyColumn = new InputColumn(DROP, false, columnNumber, dataColumn.getColumnType());
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
            InputColumn inputHeader = new InputColumn(formattedColumnId, isDropped, columnNumber, columnType);
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
                inputColumnsProvider.addCatCovTableColumn(symbol.toUpperCase()); 
            }
            if(valueType.equals(SymbolType.REAL)){
                inputColumnsProvider.addContCovTableColumn(symbol.toUpperCase());
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
