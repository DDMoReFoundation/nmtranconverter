package ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.ddmore.libpharmml.dom.commontypes.SymbolTypeType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefnType;
import eu.ddmore.libpharmml.dom.dataset.ColumnTypeType;
import eu.ddmore.libpharmml.dom.dataset.ColumnsDefinitionType;
import eu.ddmore.libpharmml.dom.dataset.DataSetType;
import eu.ddmore.libpharmml.dom.dataset.ImportDataType;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingStepsType;
import eu.ddmore.libpharmml.dom.modellingsteps.NONMEMdataSetType;

public class DataStatementTest {

	private static final String STATEMENT_BLOCK_NAME = "$DATA";
	private static final String DATA_FILE_NAME = "warfarin_conc_pca.csv";
	private static final String IGNORE_STRING = "IGNORE";
	private static final String IGNORE_CHAR = "@";
	
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


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldCreateValidDataStatementNONMEMdataSet() {

		DataSetType dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		List<ColumnDefnType> columns = columnsDefinition.getColumn();
		columns.add(createColumn(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		columns.add(createColumn(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2));
		columns.add(createColumn(COL_ID_3, COL_TYPE_3, COL_VALUE_3, COL_NUM_3));
		dataset.setDefinition(columnsDefinition);

		ModellingStepsType modellingSteps = createModellingSteps(dataset);
		
		DataStatement dataStatement = new DataStatement(modellingSteps.getNONMEMdataSet());

		assertNotNull("DataStatement should not be null.", dataStatement);
		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, dataStatement.getInputHeaders());
		assertEquals("dataFileName should be correct.", DATA_FILE_NAME, dataStatement.getDataFileName());
		assertEquals("DataStatement should be correct.",
				STATEMENT_BLOCK_NAME + " " + DATA_FILE_NAME + " " + IGNORE_STRING + "=" + IGNORE_CHAR, dataStatement.getDataStatement());
	}

	@Test
	public void shouldCreateValidDataStatementNONMEMdataSetLowerCaseColumnIds() {

		DataSetType dataset = createDataSet();

		ColumnsDefinitionType columnsDefinition = new ColumnsDefinitionType();
		List<ColumnDefnType> columns = columnsDefinition.getColumn();
		columns.add(createColumn(COL_ID_1.toLowerCase(), COL_TYPE_1, COL_VALUE_1, COL_NUM_1));
		columns.add(createColumn(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2));
		columns.add(createColumn(COL_ID_3.toLowerCase(), COL_TYPE_3, COL_VALUE_3, COL_NUM_3));
		dataset.setDefinition(columnsDefinition);

		ModellingStepsType modellingSteps = createModellingSteps(dataset);
		
		DataStatement dataStatement = new DataStatement(modellingSteps.getNONMEMdataSet());

		assertEquals("inputHeaders should be correct.", COLUMN_HEADERS, dataStatement.getInputHeaders());
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
}