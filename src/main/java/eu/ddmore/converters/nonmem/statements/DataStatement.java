package eu.ddmore.converters.nonmem.statements;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.TabularDataset;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMappingType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationStepType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class DataStatement implements Writeable {
	
	String dataFileName = "";
	String statement;

	public String getDataFileName() {
		return dataFileName;
	}

	public DataStatement(ScriptDefinition scriptDefinition, String modelName) {

		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));

		if (null == td) {
			throw new IllegalStateException("TabularDataset cannot be null");
		}

		dataFileName = generateDataFileName(modelName);
		composeData(scriptDefinition);
	}

		
	public DataStatement(List<NONMEMdataSetType> dataFiles) {
		
		// TODO: Handle multiple data sets

		if (null == dataFiles) {
			throw new IllegalStateException("NONMEM data set(s) cannot be null");
		}

		// TODO: Handle multiple data sets

		Iterator<NONMEMdataSetType> dsIterator = dataFiles.iterator();

		if (!dsIterator.hasNext()) {
			throw new IllegalStateException("NONMEM data set(s) cannot be empty");
		}

		while (dsIterator.hasNext()) {
			NONMEMdataSetType nonmemDataSet = dsIterator.next();
			// TODO: adding null check for time being as no examples for 0.3.1 or above are available right now.
			if (nonmemDataSet.getDataSet().getImportData().getPath() != null) {
				dataFileName = nonmemDataSet.getDataSet().getImportData().getPath();
			}
		}
	}

	private  String generateDataFileName(String dataFile) {
		return new File(dataFile).getName().replace(".xml", "") +"_data.csv";
	}
	
	private void composeData(ScriptDefinition scriptDefinition) {
//		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));
		
//		List<ArmIndividual> population = trialDesignBlock.getPopulation();
		
//		for (ArmIndividual arm: population){
//			System.out.println("population ARM :"+arm.getArm());
//		}
		
//		trialDesignBlock
//		for (ActivityType activityType: activityTypes){
//			System.out.println("population activity:");
//		}	
	}

	
	/**
	 * @return the printable version of this statement
	 */
	public String getStatement() {

		if (null == statement) {
			StringBuilder stringBuilder = new StringBuilder("$DATA");
			stringBuilder.append(" " + getDataFileName());
			stringBuilder.append(" " + "IGNORE=@");

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

	/**
	 * Writes this statement to the given Writer
	 */
	@Override
	public void write(Writer writer) throws IOException {
		writer.write(getStatement());
	}
}