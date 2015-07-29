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
import java.util.List;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.TabularDataset;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMapping;
import eu.ddmore.libpharmml.dom.modellingsteps.Estimation;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;

public class DataStatement{

    private static final Character IGNORE_CHAR = '@';
    private String statement;
    private String dataFileName = null;
    private File dataFile = null;
    private final ConversionContext context;

    public File getDataFile() {
        return dataFile;
    }

    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public DataStatement(ConversionContext convContext) {
        Preconditions.checkNotNull(convContext, "conversion context cannot be null");
        this.context = convContext;
    }

    private void initialiseDataStatement() {
        List<ExternalDataSet> dataFiles = context.retrieveExternalDataSets();
        if(dataFiles!=null && !dataFiles.isEmpty()){

            for (ExternalDataSet extDataSet : dataFiles) {
                if (extDataSet.getDataSet().getExternalFile().getPath() != null) {
                    String dataLocation = context.getSrcFile().getAbsoluteFile().getParentFile().getAbsolutePath();
                    dataFileName = extDataSet.getDataSet().getExternalFile().getPath();
                    File data = new File(dataLocation+File.separator+dataFileName);
                    if(data.exists()){
                        setDataFile(data);
                    }else{
                        throw new IllegalStateException("external data file doesnt exist for path :"+data.getAbsolutePath()); 
                    }
                }
            }
        }else {
            TabularDataset td = getObjectiveDatasetMap(ScriptDefinitionAccessor.getEstimationStep(context.getScriptDefinition()));

            Preconditions.checkNotNull(td, "TabularDataset cannot be null");

            dataFileName = generateDataFileName(context.getSrcFile().getAbsolutePath());
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
        initialiseDataStatement();
        
        if (null == statement) {
            StringBuilder stringBuilder = new StringBuilder(Formatter.data());
            stringBuilder.append("\"" + getDataFileName() + "\"");
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
}