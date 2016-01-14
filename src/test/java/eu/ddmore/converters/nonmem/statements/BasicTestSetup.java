package eu.ddmore.converters.nonmem.statements;

import org.junit.Before;
import org.mockito.Mock;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
/**
 * This class is used as basic common test setup for unit tests around nmtran converter classes
 */
public class BasicTestSetup {

    @Mock ConversionContext context;
    @Mock ScriptDefinition scriptDefinition;

    @Mock EstimationStep estStep;
    
    @Before
    public void setup(){
    }
    
    protected static final String DROP = "DROP";

    protected static final String COL_ID_1 = ColumnConstant.ID.toString();
    protected static final ColumnType COL_TYPE_1 = ColumnType.ID;
    protected static final SymbolType COL_VALUE_1 = SymbolType.ID;
    protected static final Integer COL_NUM_1 = new Integer(1);
    protected static final ColumnDefinition ID = new ColumnDefinition(COL_ID_1, COL_TYPE_1, COL_VALUE_1, COL_NUM_1);

    protected static final String COL_ID_2 = ColumnConstant.TIME.toString();
    protected static final ColumnType COL_TYPE_2 = ColumnType.IDV;
    protected static final SymbolType COL_VALUE_2 = SymbolType.ID;
    protected static final Integer COL_NUM_2 = new Integer(2);
    protected static final ColumnDefinition TIME = new ColumnDefinition(COL_ID_2, COL_TYPE_2, COL_VALUE_2, COL_NUM_2);

    protected static final String COL_ID_3 = "WT";
    protected static final ColumnType COL_TYPE_3 = ColumnType.UNDEFINED;
    protected static final SymbolType COL_VALUE_3 = SymbolType.ID;
    protected static final Integer COL_NUM_3 = new Integer(3);
    protected static final ColumnDefinition WT = new ColumnDefinition(COL_ID_3, COL_TYPE_3, COL_VALUE_3, COL_NUM_3);

    protected static final String COL_ID_4 = "AMT";
    protected static final ColumnType COL_TYPE_4 = ColumnType.COVARIATE;
    protected static final SymbolType COL_VALUE_4 = SymbolType.ID;
    protected static final Integer COL_NUM_4 = new Integer(4);
    protected static final ColumnDefinition AMT = new ColumnDefinition(COL_ID_4, COL_TYPE_4, COL_VALUE_4, COL_NUM_4);

    protected static final String COL_ID_5 = "EVID";
    protected static final ColumnType COL_TYPE_5 = ColumnType.UNDEFINED;
    protected static final SymbolType COL_VALUE_5 = SymbolType.ID;
    protected static final Integer COL_NUM_5 = new Integer(5);
    protected static final ColumnDefinition EVID = new ColumnDefinition(COL_ID_5, COL_TYPE_5, COL_VALUE_5, COL_NUM_5);
}
