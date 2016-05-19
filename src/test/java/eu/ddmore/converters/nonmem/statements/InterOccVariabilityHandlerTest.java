/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.spi.blocks.ParameterBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler.OccRandomVariable;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.commontypes.LevelReference;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

@PrepareForTest(RandomVariableHelper.class)
public class InterOccVariabilityHandlerTest extends BasicTestSetup {

    private static final String PARAM_VAR = "Param_Variable";
    @Mock ParameterRandomVariable variable;
    @Mock LevelReference levelRef; 
    @Mock SymbolRef levelRefSymbRef;
    @Mock ParameterBlock block;

    InterOccVariabilityHandler occVariabilityHandler;

    private List<ParameterBlock> paramBlocks = new ArrayList<>();
    private List<ParameterRandomVariable> randomVariables = new ArrayList<>();
    private List<InputColumn> columns = new ArrayList<>();
    private List<LevelReference> varReferences = new ArrayList<>();
    
    private final String columnName = "COLUMN";
    private final String OMEGA_NAME = "OMEGA";
    private InputColumn column = new InputColumn(columnName, false, 1, ColumnType.OCCASION);

    @Before
    public void setUp() throws Exception {

        mockStatic(RandomVariableHelper.class);
        context = mock(ConversionContext.class,RETURNS_DEEP_STUBS);

        mockColumns();
        mockRandomVariables();

        when(block.getRandomVariables()).thenReturn(randomVariables);
        paramBlocks.add(block);

        when(scriptDefinition.getParameterBlocks()).thenReturn(paramBlocks);
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);

        when(context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders()).thenReturn(columns);
    }

    private void mockRandomVariables() {
        when(RandomVariableHelper.getNameFromParamRandomVariable(variable)).thenReturn(OMEGA_NAME);
        when(variable.getSymbId()).thenReturn(PARAM_VAR);
        varReferences.add(levelRef);
        when(variable.getListOfVariabilityReference()).thenReturn(varReferences);
        randomVariables.add(variable);
    }

    private void mockColumns() {
        when(levelRefSymbRef.getSymbIdRef()).thenReturn(column.getColumnId());
        when(levelRef.getSymbRef()).thenReturn(levelRefSymbRef);
        columns.add(column);
    }

    @Test
    public void shouldGetRandomVarsWithOccasion() throws IOException {
        occVariabilityHandler = new InterOccVariabilityHandler(context);
        Map<Integer, OccRandomVariable> iovRandomVars = occVariabilityHandler.getRandomVarsWithOccasion();

        assertNotNull("Should return IOV random variables map ", iovRandomVars);

        OccRandomVariable occVariable = iovRandomVars.get(1);
        assertEquals("Should add omega name as expected", OMEGA_NAME, occVariable.getEta().getOmegaName());
        assertEquals("Should add parameter variable", PARAM_VAR, occVariable.getVariable().getSymbId());
    }

    @Test
    public void shouldReturnTrueIfRandomVarIOV() throws IOException {
        occVariabilityHandler = new InterOccVariabilityHandler(context);
        assertTrue("Should return as true for iov random variable", occVariabilityHandler.isRandomVarIOV(variable));
    }
}
