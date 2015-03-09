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
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.commontypes.SymbolTypeType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefnType;
import eu.ddmore.libpharmml.dom.dataset.ColumnTypeType;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMappingType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationStepType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class InputStatement {
	
	private final List<String> catCovTableColumns = new ArrayList<String>();
	private final List<String> contCovTableColumns = new ArrayList<String>();
	private List<String> inputHeaders = new ArrayList<String>();
	private String statement;
	
	public List<String> getInputHeaders() {
		return inputHeaders;
	}

	public InputStatement(ScriptDefinition scriptDefinition) {

		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));

		if (null == td) {
			throw new IllegalStateException("TabularDataset cannot be null");
		}

		computeEstimationHeaders(td);
	}

	public InputStatement(List<NONMEMdataSetType> dataFiles) {

		if (null == dataFiles) {
			throw new IllegalStateException("NONMEM data set(s) cannot be null");
		}

		Iterator<NONMEMdataSetType> dsIterator = dataFiles.iterator();
		if (!dsIterator.hasNext()) {
			throw new IllegalStateException("NONMEM data set(s) cannot be empty");
		}
		while (dsIterator.hasNext()) {
			computeEstimationHeaders(dsIterator.next());
		}
	}

	/** 
	 * Estimation headers for non-NONMEM datasets
	 * 
	 * @param td the TabularDataset to compute the estimation headers from
	 */
	private void computeEstimationHeaders(TabularDataset td) {

		List<String> dataColumns = td.getColumnNames();

		if (null == dataColumns) {
			throw new IllegalStateException("Objective data set has no columns");
		} else {
			for (String dataColumn: dataColumns) {
				inputHeaders.add(dataColumn);
			}
		}
	}

	/**
	 * Estimation headers for NONMEM datasets
	 * 
	 * @param nonmemDataSet the NONMEMdataSetType to compute the estimation headers from
	 */
	private void computeEstimationHeaders(NONMEMdataSetType nonmemDataSet) {

		List<ColumnDefnType> dataColumns = nonmemDataSet.getDataSet().getDefinition().getColumn();

		if (null == dataColumns) {
			throw new IllegalStateException("NONMEM data set has no columns");
		} else {
			for (ColumnDefnType dataColumn : dataColumns) {
				String colId = dataColumn.getColumnId().toUpperCase();
				ColumnTypeType columnType = dataColumn.getColumnType();
				SymbolTypeType valueType =  dataColumn.getValueType();
				
				if (inputHeaders.contains(colId)) {
					throw new IllegalStateException("NONMEM data set contains duplicate columns");
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
	public void populateCovTableDetails(ColumnTypeType columnType, SymbolTypeType valueType, String symbol){
		if(columnType.equals(ColumnTypeType.COVARIATE)){
			if(valueType.equals(SymbolTypeType.INT)){
			catCovTableColumns.add(symbol.toUpperCase());	
			}
			if(valueType.equals(SymbolTypeType.REAL)){
			contCovTableColumns.add(symbol.toUpperCase());
			}
		}
	}

	/**
	 * @return the printable version of this statement
	 */
	public String getStatement() {

		if (null == statement) {
			StringBuilder stringBuilder = new StringBuilder("$INPUT");

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
	    	EstimationStepType stepType = estimateStep.getStep();
	    	if(stepType.getObjectiveDataSet()!=null){
				for (DatasetMappingType dsm : stepType.getObjectiveDataSet()) {
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