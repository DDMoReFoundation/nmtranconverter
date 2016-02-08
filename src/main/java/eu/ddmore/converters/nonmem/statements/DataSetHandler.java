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
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;


public class DataSetHandler {

    private static final Character IGNORE_CHAR = '@';
    private String dataFileName = null;
    private File dataFile = null;
    private final List<ExternalDataSet> extDataSets;
    private Character ignoreChar;

    public DataSetHandler(List<ExternalDataSet> extDataSets, String dataLocation) {
        Preconditions.checkNotNull(extDataSets, "External DataSet cannot be null");
        Preconditions.checkArgument(!extDataSets.isEmpty(), "External DataSet cannot be empty");
        Preconditions.checkNotNull(dataLocation, "source file cannot be null");
        this.extDataSets= extDataSets;
        initialiseDataSetDetails(dataLocation);
    }

    private void initialiseDataSetDetails(String dataLocation) {

        if(extDataSets.size()>1){
            throw new IllegalStateException("Multiple external datasets are not supported yet.");
        }
        ExternalDataSet extDataSet = extDataSets.get(0);
        if (extDataSet.getDataSet().getExternalFile().getPath() != null) {
            dataFileName = extDataSet.getDataSet().getExternalFile().getPath();
            File data = new File(dataLocation, dataFileName);
            if(data.exists()){
                setDataFile(data);
            }else{
                throw new IllegalStateException("external data file doesnt exist for path :"+data.getAbsolutePath()); 
            }
        }

        analyseDataSet();
    }

    private void analyseDataSet() {
        try(BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(getDataFile())));) {
            String firstLine = reader.readLine();
            setIgnoreChar(getIgnoreCharacter(firstLine));
        } catch (IOException e) {
            throw new IllegalStateException("Exception while accessing data file "+dataFile.getName()+" :"+ e);
        }
    }

    /**
     * The ignore character is determined with help of first character of data file.
     * If the first char is alpha-numeric then '@' is returned 
     * or else the first char specified is returned as ignore character.
     *  
     * @param firstLine
     * @return ignore character
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Character getIgnoreCharacter(String firstLine) {
        Character firstChar = IGNORE_CHAR;
        if(!StringUtils.isEmpty(firstLine)){
            //We need only first character of first line.
            firstChar= new Character(firstLine.charAt(0));
        }
        if(Character.isLetterOrDigit(firstChar)){
            firstChar = IGNORE_CHAR;
        }
        return firstChar;
    }

    public File getDataFile() {
        return dataFile;
    }

    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public Character getIgnoreChar() {
        return ignoreChar;
    }

    public void setIgnoreChar(Character ignoreChar) {
        this.ignoreChar = ignoreChar;
    }

}
