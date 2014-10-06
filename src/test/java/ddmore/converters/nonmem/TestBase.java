/*******************************************************************************
 * Copyright (C) 2014 Cyprotex Discovery Ltd - All rights reserved.
 ******************************************************************************/

package ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;


public abstract class TestBase {
	protected ConverterProvider c = null;
	protected boolean fixedRunId = true, useCrxReportDetail = true;
	protected String inputXMLFile = null;
	protected String output_directory = "../output", expected_output_file1 = null, expected_output_file2 = null, expected_output_file3 = null;
	protected File dir = null, f = null;
	protected String interpreter_path =  "rscript.exe", command_format = "%s";
	
	public TestBase() {
		dir = new File(output_directory);
		deleteAllFiles();
	}
	
	public abstract void setUp() throws Exception;
	
	protected void deleteAllFiles() {
		for(File file: dir.listFiles()) file.delete();
	}
	
	protected void init() throws NullPointerException, IOException {
		f = new File(inputXMLFile);
		c = new ConverterProvider();
		ConverterProvider.getManager().setFixedRunId(fixedRunId);
		c.setUseCrxImplConversionReport(useCrxReportDetail);
		c.setAddPlottingBlock(false);
	}
}
