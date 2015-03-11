/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.DataStatement;
import eu.ddmore.converters.nonmem.statements.InputStatement;
import eu.ddmore.libpharmml.dom.dataset.DataSetType;
import eu.ddmore.libpharmml.dom.dataset.ImportDataType;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingStepsType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class DataStatementTest {

	private static final String DATA_FILE_NAME = "warfarin_conc_pca.csv";
	private static final String IGNORE_STRING = "IGNORE";
	private static final String IGNORE_CHAR = "@";
	File srcFile = null;

	@Test
	public void shouldCreateValidDataStatementNONMEMdataSet() {

		ModellingStepsType modellingSteps = createModellingSteps(createDataSet());
		
		srcFile = new File(DATA_FILE_NAME);
		
		//TODO : we need to create test suite which will have test data which will allow data statement to be created. 
//		DataStatement dataStatement = new DataStatement(modellingSteps.getNONMEMdataSet(),srcFile);

//		assertNotNull("DataStatement should not be null.", dataStatement);
//		assertEquals("dataFileName should be correct.", DATA_FILE_NAME, dataStatement.getDataFileName());
//		assertEquals("DataStatement should be correct.",STATEMENT_BLOCK_NAME + " " + DATA_FILE_NAME + " " + IGNORE_STRING + "=" + IGNORE_CHAR, dataStatement.getStatement());
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionNullNONMEMdataSet() {

		new InputStatement((List<NONMEMdataSetType>)null);
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionEmptyNONMEMdataSet() {

		new InputStatement((new ArrayList<NONMEMdataSetType>()));
	}

	private DataSetType createDataSet() {

		ImportDataType importData = new ImportDataType();
		importData.setPath(DATA_FILE_NAME);

		DataSetType dataset = new DataSetType();
		dataset.setImportData(importData);
		
		return dataset;
	}

	private ModellingStepsType createModellingSteps(DataSetType dataset) {

		NONMEMdataSetType nonmemDataSet = new NONMEMdataSetType();
		nonmemDataSet.setDataSet(dataset);

		ModellingStepsType modellingSteps = new ModellingStepsType();
		modellingSteps.getNONMEMdataSet().add(nonmemDataSet);

		return modellingSteps;
	}
}