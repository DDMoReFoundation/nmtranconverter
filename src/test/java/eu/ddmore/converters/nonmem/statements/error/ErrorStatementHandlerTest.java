/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.error;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.engine.common.MultipleDvRef;
import crx.converter.spi.blocks.ObservationBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.error.ErrorStatementHandler;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatement;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatementEmitter;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError.ErrorModel;

/**
 * Junit tests for ErrorStatementHandler class. 
 */
@PrepareForTest({ErrorStatementHandler.class, ScriptDefinitionAccessor.class})
public class ErrorStatementHandlerTest extends BasicTestSetup {

    private final String ERROR_STATEMENT_CONTENT = Formatter.endline()+"error";
    private final String varDefStatementString = new String("K = (CL/V)");
    @Mock private StructuralObsErrorStatement structObsErrorStatement;
    @Mock private StructuralObsErrorStatementEmitter errorStatementEmitter;
    @Mock private MultipleDvRef dvReference;
    @Mock private SymbolRef columnName;
    @Mock private DiffEquationStatementBuilder desBuilder;

    @Mock private ObservationBlock observationBlock;

    private ErrorStatementHandler errorStatementHandler;
    private Map<String, String> varDefs = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        mockStatic(ScriptDefinitionAccessor.class);

        context = mock(ConversionContext.class,RETURNS_DEEP_STUBS);
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);

        mockErrorStatementsPreperation();

        whenNew(StructuralObsErrorStatementEmitter.class).withArguments(Matchers.any(StructuralObsErrorStatement.class)).thenReturn(errorStatementEmitter);
        when(errorStatementEmitter.getErrorStatementDetails()).thenReturn(new StringBuilder(ERROR_STATEMENT_CONTENT));
    }

    private void mockErrorStatementsPreperation() throws Exception{
        List<ObservationBlock> obsBlocks = new ArrayList<>();
        obsBlocks.add(observationBlock);

        when(scriptDefinition.getObservationBlocks()).thenReturn(obsBlocks);
        StructuredObsError errorType = Mockito.mock(StructuredObsError.class, RETURNS_DEEP_STUBS);
        when(errorType.getSymbId()).thenReturn("Y");
        when(observationBlock.getObservationError()).thenReturn(errorType);

        ErrorModel errorModel = Mockito.mock(ErrorModel.class,RETURNS_DEEP_STUBS);
        FunctionCallType callType = Mockito.mock(FunctionCallType.class);
        when(errorType.getErrorModel()).thenReturn(errorModel);
        when(errorType.getOutput().getSymbRef().getSymbIdRef()).thenReturn("Y");
        when(errorModel.getAssign().getFunctionCall()).thenReturn(callType);

        when(structObsErrorStatement.isStructuralObsError()).thenReturn(true);
        when(structObsErrorStatement.getErrorStatement()).thenReturn(new StringBuilder(ERROR_STATEMENT_CONTENT));
        whenNew(StructuralObsErrorStatement.class).withAnyArguments().thenReturn(structObsErrorStatement);
    }

    @Test
    public void shouldGetErrorStatementWithNoParameters() throws Exception {
        errorStatementHandler = new ErrorStatementHandler(context);
        String errorStatement = errorStatementHandler.getErrorStatement().trim();
        assertNotNull("Error statement should not be null", errorStatement);
        assertEquals("expected Error statement should be returned",ERROR_STATEMENT_CONTENT.trim(), errorStatement);
    }

    private void setUpForErrorStatementWithDes(){
        when(desBuilder.getVariableDefinitionsStatement(varDefs)).thenReturn(new StringBuilder(varDefStatementString));
    }

    @Test
    public void shouldGetErrorStatementWithDes(){
        setUpForErrorStatementWithDes();
        errorStatementHandler = new ErrorStatementHandler(context);
        String errorStatement = errorStatementHandler.getErrorStatement(desBuilder).trim();
        assertNotNull("Error statement should not be null", errorStatement) ;
        assertEquals("expected Error statement should be returned",varDefStatementString+ERROR_STATEMENT_CONTENT, errorStatement);
    }

    private void multipleDVSetup(){
        List<MultipleDvRef> multipleDvReferences = new ArrayList<MultipleDvRef>();
        multipleDvReferences.add(dvReference);

        when(columnName.getSymbIdRef()).thenReturn(COL_ID_1);
        when(ScriptDefinitionAccessor.getAllMultipleDvReferences(scriptDefinition)).thenReturn(multipleDvReferences);
        when(context.getConditionalEventHandler().getDVColumnReference(Matchers.any(MultipleDvRef.class))).thenReturn(columnName);
    }

    @Test
    public void shouldGetErrorStatementWithMultipleDV(){
        multipleDVSetup();
        errorStatementHandler = new ErrorStatementHandler(context);
        String errorStatement = errorStatementHandler.getErrorStatement().trim();
        assertNotNull("Error statement should not be null", errorStatement);
        assertEquals("expected Error statement should be returned",ERROR_STATEMENT_CONTENT.trim(), errorStatement);
    }
}
