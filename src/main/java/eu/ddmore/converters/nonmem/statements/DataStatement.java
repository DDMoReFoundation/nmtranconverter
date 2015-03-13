/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.TabularDataset;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMappingType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationStepType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class DataStatement{
	
	private static final Character IGNORE_CHAR = '@';
	String statement;
	String dataFileName = "";
	File dataFile = null;
	String delimSybol;
	
	public File getDataFile() {
		return dataFile;
	}

	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}

	public String getDataFileName() {
		return dataFileName;
	}

	public DataStatement(ScriptDefinition scriptDefinition, File srcFile) {

		TabularDataset td = getObjectiveDatasetMap(ParametersHelper.getEstimationStep(scriptDefinition));

		if (null == td) {
			throw new IllegalStateException("TabularDataset cannot be null");
		}

		dataFileName = generateDataFileName(srcFile.getAbsolutePath());
	}
		
	public DataStatement(List<NONMEMdataSetType> dataFiles, File srcFile) {
		
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
				String dataLocation = srcFile.getAbsoluteFile().getParentFile().getAbsolutePath();
				dataFileName = nonmemDataSet.getDataSet().getImportData().getPath();
				File data = new File(dataLocation+File.separator+dataFileName);
				if(data.exists()){
					setDataFile(data);
				}else{
					throw new IllegalStateException("NONMEM data file doesnt exist"); 
				}
			}
		}
	}

	private  String generateDataFileName(String dataFile) {
		return new File(dataFile).getName().replace(".xml", "") +"_data.csv";
	}
	
	/**
	 * This method will return the data statement.
	 * The data file name is retrieved from nonmem dataset 
	 * and ignore character is determined with help of first character of data file.
	 * 
	 * @return the printable version of this statement
	 * @throws IOException 
	 */
	public String getStatement() throws IOException {
		if (null == statement) {
			StringBuilder stringBuilder = new StringBuilder(Formatter.data());
			stringBuilder.append(getDataFileName());
			stringBuilder.append(" IGNORE="+getIgnoreCharacter());
			statement = stringBuilder.toString();
		}
		return statement;
	}

	/**
	 * The ignore character is determined with help of first character of data file.
	 * If the first char is alpha-numeric then '@' is returned 
	 * or else the first char specified is returned as ignore character. 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Character getIgnoreCharacter() throws FileNotFoundException,
			IOException {
		Character firstChar = IGNORE_CHAR;
		FileInputStream inputStream = new FileInputStream(getDataFile());
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String firstLine = reader.readLine();
		if(firstLine!=null){
			//We need only first character of first line.
			firstChar= firstLine.toCharArray()[0];
		}
		if(Character.isLetterOrDigit(firstChar)){
			firstChar = IGNORE_CHAR;
		}
		reader.close();
		return firstChar;
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
}