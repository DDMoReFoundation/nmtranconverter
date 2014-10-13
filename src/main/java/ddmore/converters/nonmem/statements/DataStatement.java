package ddmore.converters.nonmem.statements;

import java.io.File;
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

public class DataStatement implements Writeable {
	
	List<String> inputHeaders = new ArrayList<String>();
	String dataFileName = "";
	
	public String getDataFileName() {
		return dataFileName;
	}

	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}

	public List<String> getInputHeaders() {
		return inputHeaders;
	}

	public DataStatement(ScriptDefinition scriptDefinition, String modelName) {

		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));

		if (null == td) {
			throw new IllegalStateException("TabularDataset cannot be null");
		}

		computeEstimationHeaders(td);
		dataFileName = generateDataFileName(modelName);
		composeData(scriptDefinition);
	}

		
	public DataStatement(List<NONMEMdataSetType> dataFiles) {
		
		// TODO: Handle multiple data sets

		Iterator<NONMEMdataSetType> dsIterator = dataFiles.iterator();

		while (dsIterator.hasNext()) {
			NONMEMdataSetType nonmemDataSet = dsIterator.next();
			// TODO: adding null check for time being as no examples for 0.3.1 or above are available right now.
			if (nonmemDataSet.getDataSet().getImportData().getPath() != null) {
				dataFileName = nonmemDataSet.getDataSet().getImportData().getPath();
			}

			computeEstimationHeaders(nonmemDataSet);
		}
	}

	private  String generateDataFileName(String dataFile) {
		return new File(dataFile).getName().replace(".xml", "") +"_data.csv";
	}
	
	// TODO: it looks like there is a major overlap between this method and
	// computeEstimationHeaders(TabularDataset td)
	private void composeData(ScriptDefinition scriptDefinition) {
		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));
		
		for (String columnName : td.getColumnNames()){
			inputHeaders.add(columnName+" ");
		}
		inputHeaders.add("AMT");
		
//		List<ArmIndividual> population = trialDesignBlock.getPopulation();
		
//		for (ArmIndividual arm: population){
//			System.out.println("population ARM :"+arm.getArm());
//		}
		
//		trialDesignBlock
//		for (ActivityType activityType: activityTypes){
//			System.out.println("population activity:");
//		}
		
	}

	// TODO: it looks like there is a major overlap between this method and
	// composeData(ScriptDefinition scriptDefinition)
	/** 
	 * Estimation headers for non-NONMEM datasets
	 * 
	 * @param td the TabularDataset to compute the estimation headers from
	 */
	private void computeEstimationHeaders(TabularDataset td) {

		List<String> dataColumns = td.getColumnNames();

		if (dataColumns != null) {
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

		if (dataColumns != null) {
			for (ColumnDefnType dataColumn : dataColumns) {
				inputHeaders.add(dataColumn.getColumnId().toUpperCase());
			}
		}
	}

	/**
	 * Returns data statement for the given data filename 
	 * @return
	 */
	public String getDataStatement(){
		
		String dataStatement = getDataFileName() + " IGNORE=@";
		//TODO : throw exception if null
		return ("$DATA "+ dataStatement);
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
		writer.write(getDataStatement());
	}
}