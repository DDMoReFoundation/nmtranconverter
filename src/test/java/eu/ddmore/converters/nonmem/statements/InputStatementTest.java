package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.InputStatement;
import eu.ddmore.libpharmml.dom.commontypes.SymbolTypeType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefnType;
import eu.ddmore.libpharmml.dom.dataset.ColumnTypeType;
import eu.ddmore.libpharmml.dom.dataset.ColumnsDefinitionType;
import eu.ddmore.libpharmml.dom.dataset.DataSetType;
import eu.ddmore.libpharmml.dom.dataset.ImportDataType;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingStepsType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class InputStatementTest {

	private static final String STATEMENT_BLOCK_NAME = "$INPUT";
	private static final String DATA_FILE_NAME = "warfarin_conc_pca.csv";
	
	private static final String COL_ID_1 = "ID";
	private static final ColumnTypeType COL_TYPE_1 = ColumnTypeType.ID;
	private static final SymbolTypeType COL_VALUE_1 = SymbolTypeType.ID;
	private static final String COL_NUM_1 = "1";

	private static final String COL_ID_2 = "TIME";
	private static final ColumnTypeType COL_TYPE_2 = ColumnTypeType.IDV;
	private static final SymbolTypeType COL_VALUE_2 = SymbolTypeType.ID;
	private static final String COL_NUM_2 = "2";
	
	private static final String COL_ID_3 = "WT";
	private static final ColumnTypeType COL_TYPE_3 = ColumnTypeType.COVARIATE;
	private static final SymbolTypeType COL_VALUE_3 = SymbolTypeType.ID;
	private static final String COL_NUM_3 = "3";

	private static final List<String> COLUMN_HEADERS = Arrays.asList(COL_ID_1, COL_ID_2, COL_ID_3);


	@Test
	public void shouldCreateValidInputStatementNONMEMdataSet() {

		DataSetType dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		List<ColumnDefnType> columns = columnsDefinition.getColumn();
		columns.add(createColumn(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		columns.add(createColumn(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2));
		columns.add(createColumn(COL_ID_3, COL_TYPE_3, COL_VALUE_3, COL_NUM_3));
		dataset.setDefinition(columnsDefinition);

		ModellingStepsType modellingSteps = createModellingSteps(dataset);
		
		InputStatement statement = new InputStatement(modellingSteps.getNONMEMdataSet());

		assertNotNull("InputStatement should not be null.", statement);
		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, statement.getInputHeaders());
		assertEquals("InputStatement should be correct.",
				STATEMENT_BLOCK_NAME + getFormattedColumnHeaders(), statement.getStatement());
	}

	@Test
	public void shouldCreateValidInputStatementNONMEMdataSetLowerCaseColumnIds() {

		DataSetType dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		List<ColumnDefnType> columns = columnsDefinition.getColumn();
		columns.add(createColumn(COL_ID_1.toLowerCase(), COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		columns.add(createColumn(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2));
		columns.add(createColumn(COL_ID_3.toLowerCase(), COL_TYPE_3, COL_VALUE_3, COL_NUM_3));
		dataset.setDefinition(columnsDefinition);

		ModellingStepsType modellingSteps = createModellingSteps(dataset);

		InputStatement statement = new InputStatement(modellingSteps.getNONMEMdataSet());

		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, statement.getInputHeaders());
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionNullNONMEMdataSet() {

		new InputStatement((List<NONMEMdataSetType>)null);
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionEmptyNONMEMdataSet() {

		new InputStatement((new ArrayList<NONMEMdataSetType>()));
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionNONMEMdataSetDuplicateColumns() {

		DataSetType dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		List<ColumnDefnType> columns = columnsDefinition.getColumn();
		columns.add(createColumn(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		columns.add(createColumn(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2));
		columns.add(createColumn(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		dataset.setDefinition(columnsDefinition);

		ModellingStepsType modellingSteps = createModellingSteps(dataset);

		new InputStatement(modellingSteps.getNONMEMdataSet());
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

	private ColumnDefnType createColumn(String id, ColumnTypeType type, SymbolTypeType value, String num) {

		ColumnDefnType cd = new ColumnDefnType();
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