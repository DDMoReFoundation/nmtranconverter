/*******************************************************************************
 * Copyright (C) 2014 Cyprotex Discovery Ltd - All rights reserved.
 ******************************************************************************/

package ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;

import org.junit.After;

import com.csvreader.CsvReader;

import crx.models.Example;

public abstract class TestBase {
	protected ConverterProvider c = null;
	protected boolean fixedRunId = true, useCrxReportDetail = true;
	protected String inputXMLFile = null;
	protected String output_directory = "../output", expected_output_file1 = null, expected_output_file2 = null, expected_output_file3 = null;
	protected File dir = null, f = null;
	protected String interpreter_path =  "rscript.exe", command_format = "%s";
	protected CsvReader data = null;
	
	public TestBase() {
		Example.echoFile = false;
		dir = new File(output_directory);
		deleteAllFiles();
	}
	
	public abstract void setUp() throws Exception;
	
	@After
	public void tearDown() throws Exception {
		if (data != null) {
			data.close();
			data = null;
		}
		
	}
	
	public abstract void test();
	
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
