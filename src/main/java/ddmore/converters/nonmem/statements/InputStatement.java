package ddmore.converters.nonmem.statements;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.TabularDataset;
import crx.converter.engine.parts.TrialDesignBlock;
import ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefnType;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMappingType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationStepType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class InputStatement implements Writeable {
	
	List<String> inputHeaders = new ArrayList<String>();
	String statement;
	
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

		// TODO: Handle multiple data sets

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
				
				if (inputHeaders.contains(colId)) {
					throw new IllegalStateException("NONMEM data set contains duplicate columns");
				} else {
					inputHeaders.add(colId);					
				}
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

	/**
	 * Writes this DataStatement to the given Writer 
	 * @return
	 */
	@Override
	public void write(Writer writer) throws IOException {
		writer.write(getStatement());
	}
}