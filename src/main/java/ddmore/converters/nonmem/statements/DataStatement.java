package ddmore.converters.nonmem.statements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.DataFiles;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.Part;
import crx.converter.engine.parts.TabularDataset;
import crx.converter.engine.parts.TrialDesignBlock;
import crx.converter.engine.parts.TrialDesignBlock.ArmIndividual;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefnType;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMappingType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationStepType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;
import eu.ddmore.libpharmml.dom.trialdesign.ActivityType;

public class DataStatement {
	
	List<String> inputHeaders = new ArrayList<String>();
	String dataFileName = new String();
	
	public String getDataFileName() {
		return dataFileName;
	}

	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}

	public List<String> getInputHeaders() {
		return inputHeaders;
	}

	public DataStatement(ScriptDefinition scriptDefinition,DataFiles dataFiles, String model_filename){
		
		TabularDataset td = getObjectiveDatasetMap(getEstimationStep(scriptDefinition));
		dataFileName = generateDataFileName(model_filename);
		if(td != null){
			computeEstimationHeaders(td);
			//create data file using data file name
			composeData(scriptDefinition);
		}else {
			//TODO: get nonmem dataset
			if(!dataFiles.getNonmemDataSets().isEmpty()){
				NONMEMdataSetType nonmemDataSet =  dataFiles.getNonmemDataSets().iterator().next();
				//TODO: adding null check for time being as no examples for 0.3.1 or above are available right now.
				if(nonmemDataSet.getDataSet().getImportData().getPath() != null){
					dataFileName = generateDataFileName(nonmemDataSet.getDataSet().getImportData().getPath());	
				}
				
				computeEstimationHeaders(nonmemDataSet);
			}
//			computeEstimationHeaders(nonmemDataSet);
			
		}
		
	}

	private  String generateDataFileName(String dataFile) {
		return new File(dataFile).getName().replace(".xml", "") +"_data.csv";
	}
	
	private void composeData(ScriptDefinition scriptDefinition) {
		TrialDesignBlock trialDesignBlock = scriptDefinition.getTrialDesignBlock();
		TabularDataset td = getObjectiveDatasetMap(getEstimationStep(scriptDefinition));
		
		for (String columnName : td.getColumnNames()){
			inputHeaders.add(columnName+" ");
		}
		inputHeaders.add("AMT");
		
		List<ArmIndividual> population = trialDesignBlock.getPopulation();
		List<ActivityType> activityTypes= trialDesignBlock.getActivities();
		
		for (ArmIndividual arm: population){
			System.out.println("population ARM :"+arm.getArm());
		}
		
//		trialDesignBlock
		for (ActivityType activityType: activityTypes){
			System.out.println("population activity:");
		}
		
	}

	/** 
	 * Estimantion headers when nonmem dataset is not available.
	 * 
	 * @param td
	 */
	private void computeEstimationHeaders(TabularDataset td) {
		inputHeaders = new ArrayList<String>();
		List<String> dataColumns = td.getColumnNames();
		if(dataColumns!=null){
			for (String dataColumn: dataColumns) {
				inputHeaders.add(dataColumn);
			}
		}
		
	}
	
	/**
	 * Estimantion headers when nonmem dataset is available.
	 * 
	 * @param nonmemDataSet
	 */
	private void computeEstimationHeaders(NONMEMdataSetType nonmemDataSet) {
		inputHeaders = new ArrayList<String>();
		List<ColumnDefnType> dataColumns = nonmemDataSet.getDataSet().getDefinition().getColumn();
		if(dataColumns!=null){
			for (ColumnDefnType dataColumn :  nonmemDataSet.getDataSet().getDefinition().getColumn()) {
				inputHeaders.add(dataColumn.getColumnId());
			}
		}
	}

	/**
	 * Returns data statement for the given data filename 
	 * @return
	 */
	public String getDataStatement(){
		
		String dataStatement = getDataFileName()+" IGNORE=@";
		return ("\n$DATA "+ dataStatement);
	}
	
    private void computeDosingHeaders(List<String> dosingColumns) {
    	inputHeaders.add("ARM");

        for (String dosingColumn : dosingColumns) {
			String columnName = ( dosingColumn == "DOSE" ? "AMT" : dosingColumn );
            if (!inputHeaders.contains(columnName)) {
            	inputHeaders.add(columnName);
            }
        }
        inputHeaders.add("MDV");
        inputHeaders.add("EVID");
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
				inputHeaders.add(columnName+" ");
			}
    	}
		
		List<ArmIndividual> population = tdblock.getPopulation();
		List<ActivityType> activityTypes= tdblock.getActivities();
		
		for (ArmIndividual arm: population){
			System.out.println(arm.getArm());
		}
		
		return inputHeaders.toString().replaceAll("\\[|\\]|\\,", "");
	}
	
	public EstimationStep getEstimationStep(ScriptDefinition scriptDefinition) {
		EstimationStep step = null;
		for (Part nextStep : scriptDefinition.getStepsMap().values()) {
			if (nextStep instanceof EstimationStep) step = (EstimationStep) nextStep; 
		}
		return step;
	}


}
