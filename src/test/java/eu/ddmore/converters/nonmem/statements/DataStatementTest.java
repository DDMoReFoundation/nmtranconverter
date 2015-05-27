/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.InputStatement;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.ExternalFile;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InputStatement.class)
public class DataStatementTest {

	private static final String DATA_FILE_NAME = "warfarin_conc_pca.csv";
	private static final String IGNORE_STRING = "IGNORE";
	private static final String IGNORE_CHAR = "@";
	File srcFile = null;
	@Mock ConversionContext context;

	@Test
	public void shouldCreateValidDataStatementNONMEMdataSet() {

		ModellingSteps modellingSteps = createModellingSteps(createDataSet());
		
		srcFile = new File(DATA_FILE_NAME);
		
		//TODO : we need to create test suite which will have test data which will allow data statement to be created. 
//		DataStatement dataStatement = new DataStatement(modellingSteps.getNONMEMdataSet(),srcFile);

//		assertNotNull("DataStatement should not be null.", dataStatement);
//		assertEquals("dataFileName should be correct.", DATA_FILE_NAME, dataStatement.getDataFileName());
//		assertEquals("DataStatement should be correct.",STATEMENT_BLOCK_NAME + " " + DATA_FILE_NAME + " " + IGNORE_STRING + "=" + IGNORE_CHAR, dataStatement.getStatement());
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionNullNONMEMdataSet() {

		new InputStatement((ConversionContext)null);
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionEmptyNONMEMdataSet() {

		new InputStatement(context);
	}

	private DataSet createDataSet() {

		ExternalFile importData = new ExternalFile();
		importData.setPath(DATA_FILE_NAME);

		DataSet dataset = new DataSet();
		dataset.setImportData(importData);
		
		return dataset;
	}

	private ModellingSteps createModellingSteps(DataSet dataset) {

		ExternalDataSet nonmemDataSet = new ExternalDataSet();
		nonmemDataSet.setDataSet(dataset);

		ModellingSteps modellingSteps = new ModellingSteps();
		modellingSteps.getListOfExternalDataSet().add(nonmemDataSet);

		return modellingSteps;
	}
}