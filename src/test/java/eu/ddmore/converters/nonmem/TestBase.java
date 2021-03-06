/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import eu.ddmore.converters.nonmem.ConverterProvider;


public abstract class TestBase {
    protected ConverterProvider c = null;
    protected boolean fixedRunId = true, useCrxReportDetail = true;
    protected String inputXMLFile = null;
    protected String inputDataFile = null;
    protected File dir = null, f = null;

    final static String TEST_DATA_DIR = "/test-models/PharmML/";
    final static String WORKING_DIR = "target/MainTest_Working_Dir/";
    final static String USECASE_DIR = "usecases/";

    final static String V_0_4_SUBDIR = "0.4.0/";
    final static String V_0_4_1_SUBDIR = "0.4.1/";
    final static String V_0_6_SUBDIR = "0.6.0/";
    final static String V_0_8_SUBDIR = "0.8.0/";
    final static String V_0_8_1_SUBDIR = "0.8.1/";


    public TestBase() {
        dir = new File(WORKING_DIR);
        if(!dir.exists()){
            dir.mkdir();
        }
        deleteAllFiles(dir);
    }

    protected void deleteAllFiles(File directory) {
        for(File file: directory.listFiles()) file.delete();
    }

    protected void init(String InputFile, String version) throws NullPointerException, IOException {
    
        f = getFile(InputFile, version);
        dir = new File(WORKING_DIR, version);

        c = new ConverterProvider();
    }

    /*
     * 
     *  f = new File(inputXMLFile);
        c = new Converter();
        Converter.getManager().setFixedRunId(fixedRunId);
        c.setUseCrxImplConversionReport(useCrxReportDetail);
        c.setAddPlottingBlock(false);
        c.setRunId("test");
        BaseLexer.getManager().setFixedRunId(true);
        job = new Job();
        job.setInterpreterExePath(interpreter_path);
        job.setCommandFormat(command_format);
     */
    /**
     * 
     * @param relativePathToFile
     * @param versionSubDirectory
     * @return
     */
    private File getFile(final String relativePathToFile, final String versionSubDirectory) {

        final URL urlToFile = TestBase.class.getResource(TEST_DATA_DIR + versionSubDirectory + relativePathToFile);
        File destFile = new File(WORKING_DIR + versionSubDirectory + relativePathToFile);

        try {
            FileUtils.copyURLToFile(urlToFile, destFile);
        } catch (IOException e) {
            System.out.println("File specified in URL cannot be found"+ e.getMessage());
        }

        return destFile;
    }
}
