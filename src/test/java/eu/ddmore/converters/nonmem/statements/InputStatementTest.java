/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.dataset.ColumnsDefinitionType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.ExternalFile;
import eu.ddmore.libpharmml.dom.modellingsteps.ExternalDataSet;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps;

@RunWith(PowerMockRunner.class)
public class InputStatementTest {

	private static final String DATA_FILE_NAME = "warfarin_conc_pca.csv";
	
	private static final String COL_ID_1 = ColumnConstant.ID.toString();
	private static final ColumnType COL_TYPE_1 = ColumnType.ID;
	private static final SymbolType COL_VALUE_1 = SymbolType.ID;
	private static final String COL_NUM_1 = "1";

	private static final String COL_ID_2 = ColumnConstant.TIME.toString();
	private static final ColumnType COL_TYPE_2 = ColumnType.IDV;
	private static final SymbolType COL_VALUE_2 = SymbolType.ID;
	private static final String COL_NUM_2 = "2";
	
	private static final String COL_ID_3 = "WT";
	private static final ColumnType COL_TYPE_3 = ColumnType.COVARIATE;
	private static final SymbolType COL_VALUE_3 = SymbolType.ID;
	private static final String COL_NUM_3 = "3";

	private static final List<String> COLUMN_HEADERS = Arrays.asList(COL_ID_1, COL_ID_2, COL_ID_3);
	
	@Mock ConversionContext context;

	@Ignore("The input statement needs to be finalised and test needs to be revised for updates")
	@Test
	public void shouldCreateValidInputStatementNONMEMdataSet() {

		DataSet dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		
		InputStatement statement = new InputStatement(context);

		assertNotNull("InputStatement should not be null.", statement);
		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, statement.getInputHeaders());
		assertEquals("InputStatement should be correct.",
		    Formatter.input() + getFormattedColumnHeaders(), statement.getStatement());
	}

	@Ignore("The input statement needs to be finalised and test needs to be revised for updates")
	@Test
	public void shouldCreateValidInputStatementNONMEMdataSetLowerCaseColumnIds() {

		DataSet dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();

		ModellingSteps modellingSteps = createModellingSteps(dataset);

		InputStatement statement = new InputStatement(context);

		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, statement.getInputHeaders());
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionNullNONMEMdataSet() {

		new InputStatement(null);
	}

	@Ignore ("The input statement needs to be finalised and test needs to be revised for updates")
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionNONMEMdataSetDuplicateColumns() {

		DataSet dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();

		ModellingSteps modellingSteps = createModellingSteps(dataset);

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

	private ColumnDefinition createColumn(String id, ColumnType type, SymbolType value, String num) {

		ColumnDefinition cd = new ColumnDefinition();
		cd.setColumnId(id);
		cd.setColumnType(type);
		cd.setValueType(value);
		cd.setColumnNum(new BigInteger(num));

		return cd;
	}
	
	private String getFormattedColumnHeaders() {

		StringBuilder headers = new StringBuilder();

		for (String header : COLUMN_HEADERS) {
			headers.append(" ");
			headers.append(header.toUpperCase());
		}
		
		return headers.toString();
	}
}