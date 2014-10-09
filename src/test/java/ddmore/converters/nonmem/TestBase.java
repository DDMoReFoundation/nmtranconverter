/*******************************************************************************
 * Copyright (C) 2014 Cyprotex Discovery Ltd - All rights reserved.
 ******************************************************************************/

package ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;


public abstract class TestBase {
	protected ConverterProvider c = null;
	protected boolean fixedRunId = true, useCrxReportDetail = true;
	protected String inputXMLFile = null;
	protected File dir = null, f = null;
	
	final static String TEST_DATA_DIR = "/eu/ddmore/testdata/models/pharmml/";
    final static String WORKING_DIR = "target/MainTest_Working_Dir";
    final static String V_0_3_SUBDIR = "v0.3/";
    final static String V_0_4_SUBDIR = "v0.4/";
	
	
	public TestBase() {
		dir = new File(WORKING_DIR);
		if(!dir.exists()){
				dir.mkdir();
		}
		deleteAllFiles(dir);
	}
	
	public abstract void setUp() throws Exception;
	
	protected void deleteAllFiles(File directory) {
		for(File file: directory.listFiles()) file.delete();
	}
	
	protected void init(String InputFile, String version) throws NullPointerException, IOException {
		
		f = getFile(InputFile,version);
		
		c = new ConverterProvider();
		ConverterProvider.getManager().setFixedRunId(fixedRunId);
		c.setUseCrxImplConversionReport(useCrxReportDetail);
		c.setAddPlottingBlock(false);
	}
	
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
