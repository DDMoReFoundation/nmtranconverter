/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.TabularDataset;
import crx.converter.engine.parts.TrialDesignBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMapping;
import eu.ddmore.libpharmml.dom.modellingsteps.Estimation;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

public class InputStatement {

    private final List<String> catCovTableColumns = new ArrayList<String>();
    private final List<String> contCovTableColumns = new ArrayList<String>();
    private List<String> inputHeaders = new ArrayList<String>();
    private String statement;

    public List<String> getInputHeaders() {
        return inputHeaders;
    }
    
    public InputStatement(ConversionContext context) {
        if (null == context) {
            throw new IllegalStateException("conversion context cannot be null");
        }
        
        List<ExternalDataSet> dataFiles = context.retrieveExternalDataSets();
        
        if(dataFiles!=null && !dataFiles.isEmpty()){
            computeEstHeadersforExternalDataSets(dataFiles);
        }else{
            computeEstHeadersforTabularDataSet(context.getScriptDefinition());
        }
        
        
    }

    /**
     * Computes estimation headers for tabular dataset retrieved from script definition.
     * 
     * @param scriptDefinition
     */
    private void computeEstHeadersforTabularDataSet(ScriptDefinition scriptDefinition) {
        TabularDataset tabularDataset = getObjectiveDatasetMap(ConversionContext.getEstimationStep(scriptDefinition));
        
        if (null == tabularDataset) {
            throw new IllegalStateException("TabularDataset cannot be null");
        }

        computeEstimationHeaders(tabularDataset);
    }

    /**
     * Computes estimation headers for external datasets retrieved.
     * 
     * @param dataFiles
     */
    private void computeEstHeadersforExternalDataSets(List<ExternalDataSet> dataFiles) {

        if (null == dataFiles) {
            throw new IllegalStateException("External data set(s) cannot be null");
        }

        Iterator<ExternalDataSet> dsIterator = dataFiles.iterator();
        if (!dsIterator.hasNext()) {
            throw new IllegalStateException("External data set(s) cannot be empty");
        }
        while (dsIterator.hasNext()) {
            computeEstimationHeaders(dsIterator.next());
        }
    }

    /** 
     * Estimation headers for tabular datasets
     * 
     * @param tabularDataset the TabularDataset to compute the estimation headers from
     */
    private void computeEstimationHeaders(TabularDataset tabularDataset) {

        List<String> dataColumns = tabularDataset.getColumnNames();

        if (null == dataColumns) {
            throw new IllegalStateException("Objective data set has no columns");
        } else {
            for (String dataColumn: dataColumns) {
                inputHeaders.add(dataColumn);
            }
        }
    }

    /**
     * Estimation headers for External datasets
     * 
     * @param externalDataSet the ExternalDataSet to compute the estimation headers from
     */
    private void computeEstimationHeaders(ExternalDataSet externalDataSet) {

        List<ColumnDefinition> dataColumns = externalDataSet.getDataSet().getListOfColumnDefinition();

        if (null == dataColumns) {
            throw new IllegalStateException("External data set has no columns");
        } else {
            for (ColumnDefinition dataColumn : dataColumns) {
                String colId = dataColumn.getColumnId().toUpperCase();
                ColumnType columnType = dataColumn.getColumnType();
                SymbolType valueType =  dataColumn.getValueType();

                if (inputHeaders.contains(colId)) {
                    throw new IllegalStateException("External data set contains duplicate columns");
                } else {
                    inputHeaders.add(colId);
                    populateCovTableDetails(columnType,valueType,colId);
                }
            }
        }
    }

    /**
     * This method populates cov table column details and creates lists of categorical cov tables and continuous cov tables.
     * These lists are used while creating table statements.
     * @param columnType
     * @param valueType
     * @param symbol
     */
    public void populateCovTableDetails(ColumnType columnType, SymbolType valueType, String symbol){
        if(columnType.equals(ColumnType.COVARIATE)){
            if(valueType.equals(SymbolType.INT)){
                catCovTableColumns.add(symbol.toUpperCase());	
            }
            if(valueType.equals(SymbolType.REAL)){
                contCovTableColumns.add(symbol.toUpperCase());
            }
        }
    }

    /**
     * @return the printable version of this statement
     */
    public String getStatement() {

        if (null == statement) {
            StringBuilder stringBuilder = new StringBuilder(Formatter.input());

            for (String nextColumn : getInputHeaders()) {
                stringBuilder.append(" " + nextColumn);
            }

            statement = stringBuilder.toString();
        }

        return statement;
    }

    private TabularDataset getObjectiveDatasetMap(EstimationStep estimateStep){
        TabularDataset dataset = null;
        if(estimateStep != null){
            Estimation stepType = estimateStep.getStep();
            if(stepType.getObjectiveDataSet()!=null){
                for (DatasetMapping dsm : stepType.getObjectiveDataSet()) {
                    //TODO: we return first occurrence of the element assuming that there is only one 
                    //		but need to handle it in better way in future. 
                    if (dsm != null) {
                        dataset = estimateStep.getObjectivDatasetMap(dsm);
                    }
                }
            }
        }
        return dataset;
    }

    public String getAllInputHeaders(EstimationStep estimateStep,TrialDesignBlock tdblock){
        TabularDataset td = null;
        if(estimateStep != null){
            td = getObjectiveDatasetMap(estimateStep);
            for (String columnName : td.getColumnNames()){
                inputHeaders.add(columnName.toUpperCase() + " ");
            }
        }
        return inputHeaders.toString().replaceAll("\\[|\\]|\\,", "");
    }

    public List<String> getCatCovTableColumns() {
        return catCovTableColumns;
    }

    public List<String> getContCovTableColumns() {
        return contCovTableColumns;
    }
}