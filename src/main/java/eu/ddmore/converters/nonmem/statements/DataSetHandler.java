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
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;


public class DataSetHandler {

    private static final Character IGNORE_CHAR = '@';
    private String dataFileName = null;
    private File dataFile = null;
    private final List<ExternalDataSet> extDataSets;
    private final File srcFile;
    private Character ignoreChar;

    public DataSetHandler(List<ExternalDataSet> extDataSets, File srcFile) throws IOException {
        Preconditions.checkNotNull(extDataSets, "External DataSet cannot be null");
        Preconditions.checkArgument(!extDataSets.isEmpty(), "External DataSet cannot be empty");
        Preconditions.checkNotNull(srcFile, "source file cannot be null");
        this.extDataSets= extDataSets;
        this.srcFile = srcFile;
        initialiseDataSetDetails();
    }

    private void initialiseDataSetDetails() throws IOException {

        for (ExternalDataSet extDataSet : extDataSets) {
            if (extDataSet.getDataSet().getExternalFile().getPath() != null) {
                String dataLocation = srcFile.getAbsoluteFile().getParentFile().getAbsolutePath();
                dataFileName = extDataSet.getDataSet().getExternalFile().getPath();
                File data = new File(dataLocation+File.separator+dataFileName);
                if(data.exists()){
                    setDataFile(data);
                }else{
                    throw new IllegalStateException("external data file doesnt exist for path :"+data.getAbsolutePath()); 
                }
            }
        }

        analyseDataSet();
    }

    private void analyseDataSet() throws IOException{
        FileInputStream inputStream = new FileInputStream(getDataFile());
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String firstLine = reader.readLine();
        setIgnoreChar(getIgnoreCharacter(firstLine));

        reader.close();
    }

//    /**
//     * Sets 
//     * @param iovColumnUniqueValues
//     * @throws IOException
//     */
//    public void setIovColumnUniqueValues(Map<String, List<Double>> iovColumnUniqueValues) throws IOException{
//
//        CsvReader reader = new CsvReader(getDataFile().getAbsolutePath());
//        reader.readHeaders();
//        while (reader.readRecord())
//        {
//            for(String columnName : iovColumnUniqueValues.keySet()){
//                List<Double> uniqueValues = iovColumnUniqueValues.get(columnName);
//                Double value = Double.parseDouble(reader.get(columnName));
//                if(!uniqueValues.contains(value)){
//                    uniqueValues.add(value);
//                }
//            }
//        }
//        reader.close();
//    }

    private Character getIgnoreCharacter(String firstLine) throws FileNotFoundException,IOException {
        Character firstChar = IGNORE_CHAR;
        if(!StringUtils.isEmpty(firstLine)){
            //We need only first character of first line.
            firstChar= firstLine.toCharArray()[0];
        }
        if(Character.isLetterOrDigit(firstChar)){
            firstChar = IGNORE_CHAR;
        }
        return firstChar;
    }

    /**
     * The ignore character is determined with help of first character of data file.
     * If the first char is alpha-numeric then '@' is returned 
     * or else the first char specified is returned as ignore character. 
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    //    public Character getIgnoreCharacter() throws FileNotFoundException,IOException {
    //        Character firstChar = IGNORE_CHAR;
    //        FileInputStream inputStream = new FileInputStream(getDataFile());
    //        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    //        String firstLine = reader.readLine();
    //        if(firstLine!=null){
    //            //We need only first character of first line.
    //            firstChar= firstLine.toCharArray()[0];
    //        }
    //        if(Character.isLetterOrDigit(firstChar)){
    //            firstChar = IGNORE_CHAR;
    //        }
    //        reader.close();
    //        return firstChar;
    //    }

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
