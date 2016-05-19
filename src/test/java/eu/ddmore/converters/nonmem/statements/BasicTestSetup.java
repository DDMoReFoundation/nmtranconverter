/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.when;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.steps.EstimationStep;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.parameters.ParametersBuilder;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
/**
 * This class is used as basic common test setup for unit tests around nmtran converter classes
 */
@RunWith(PowerMockRunner.class)
public class BasicTestSetup {

    @Mock
    protected ConversionContext context;

    @Mock
    protected ParametersBuilder parametersHelper;

    @Mock 
    protected ScriptDefinition scriptDefinition;
    
    @Mock 
    protected LocalParserHelper localParserHelper; 

    @Mock 
    protected EstimationStep estStep;

    @Before
    public void setup(){
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);
        when(context.getLocalParserHelper()).thenReturn(localParserHelper);
    }

    protected static final String PRED_EXAMPLE= Formatter.endline("PRED BLOCK");
    protected static final String IDV_EXAMPLE = Formatter.endline(Formatter.endline("MU_1 = LOG(POP_KA)")+"KA =  EXP(MU_1 +  ETA(1)) ;");
    protected static final String ERROR_EXAMPLE = Formatter.endline("error");
    protected static final String VAR_DEF_EXAMPLE = Formatter.endline("K = (CL/V)");
    protected static final String COMP1_EXAMPLE = "COMP (COMP1) "+Formatter.indent(";ID");

    protected static final String DROP = "DROP";

    protected static final String COL_ID_1 = ColumnConstant.ID.toString();
    protected static final ColumnType COL_TYPE_1 = ColumnType.ID;
    protected static final SymbolType COL_VALUE_1 = SymbolType.ID;
    protected static final Integer COL_NUM_1 = new Integer(1);
    protected static final ColumnDefinition ID = new ColumnDefinition(COL_ID_1, COL_VALUE_1, COL_NUM_1, COL_TYPE_1);

    protected static final String COL_ID_2 = ColumnConstant.TIME.toString();
    protected static final ColumnType COL_TYPE_2 = ColumnType.IDV;
    protected static final SymbolType COL_VALUE_2 = SymbolType.ID;
    protected static final Integer COL_NUM_2 = new Integer(2);
    protected static final ColumnDefinition TIME = new ColumnDefinition(COL_ID_2, COL_VALUE_2, COL_NUM_2, COL_TYPE_2);

    protected static final String COL_ID_3 = "WT";
    protected static final ColumnType COL_TYPE_3 = ColumnType.UNDEFINED;
    protected static final SymbolType COL_VALUE_3 = SymbolType.ID;
    protected static final Integer COL_NUM_3 = new Integer(3);
    protected static final ColumnDefinition WT = new ColumnDefinition(COL_ID_3, COL_VALUE_3, COL_NUM_3, COL_TYPE_3);

    protected static final String COL_ID_4 = "AMT";
    protected static final ColumnType COL_TYPE_4 = ColumnType.COVARIATE;
    protected static final SymbolType COL_VALUE_4 = SymbolType.INT;
    protected static final Integer COL_NUM_4 = new Integer(4);
    protected static final ColumnDefinition AMT = new ColumnDefinition(COL_ID_4, COL_VALUE_4, COL_NUM_4, COL_TYPE_4);

    protected static final String COL_ID_5 = "EVID";
    protected static final ColumnType COL_TYPE_5 = ColumnType.UNDEFINED;
    protected static final SymbolType COL_VALUE_5 = SymbolType.ID;
    protected static final Integer COL_NUM_5 = new Integer(5);
    protected static final ColumnDefinition EVID = new ColumnDefinition(COL_ID_5, COL_VALUE_5, COL_NUM_5, COL_TYPE_5);
}
