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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorType;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatement;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatement.FunctionArg;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;

@PrepareForTest(ScalarValueHandler.class)
public class ErrorStatementTest extends BasicTestSetup {

    private static final String OUTPUT_VAR = "CC";
    private static final String RUV_PROP_VAR = "RUV_PROP";
    private static final String RUV_ADD_VAR = "RUV_ADD";

    @Mock FunctionCallType functionCallType;

    FunctionArgument additiveArg;
    @Mock FunctionArgument propArg;
    @Mock FunctionArgument functionArg;

    @Mock JAXBElement<?> scalarVal;
    @Mock IntValue val;

    @Mock SymbolRef symbolRef;

    Integer value = new Integer("1");
    String output = "outputVariablePlaceHolder";
    String epsilonVar = "EPS_Y";
    StructuralObsErrorStatement errorStatement;
    List<FunctionArgument> args = new ArrayList<FunctionArgument>();

    @Before
    public void setUp() throws Exception {
        mockStatic(ScalarValueHandler.class);
        when(symbolRef.getSymbIdRef()).thenReturn(StructuralObsErrorType.COMBINED_ERROR_1.getErrorType());
        when(functionCallType.getSymbRef()).thenReturn(symbolRef);

        additiveArg = mock(FunctionArgument.class,RETURNS_DEEP_STUBS);
        when(additiveArg.getSymbRef().getSymbIdRef()).thenReturn(RUV_ADD_VAR);
        when(additiveArg.getSymbId()).thenReturn(FunctionArg.ADDITIVE.getDescription());
        args.add(additiveArg);

        propArg = mock(FunctionArgument.class,RETURNS_DEEP_STUBS);
        when(propArg.getSymbRef().getSymbIdRef()).thenReturn(RUV_PROP_VAR);
        when(propArg.getSymbId()).thenReturn(FunctionArg.PROP.getDescription());
        args.add(propArg);

        functionArg = mock(FunctionArgument.class,RETURNS_DEEP_STUBS);
        when(functionArg.getSymbRef().getSymbIdRef()).thenReturn(OUTPUT_VAR);
        when(functionArg.getSymbId()).thenReturn(FunctionArg.FUNC.getDescription());
        args.add(functionArg);

        when(functionCallType.getListOfFunctionArgument()).thenReturn(args);
    }

    @Test
    public void shouldGetFunctionArgumentDetails() {

        errorStatement = new StructuralObsErrorStatement(functionCallType, output, epsilonVar, true);

        assertNotNull("Function name should not be null", errorStatement.getFunctionName());

        assertEquals("Should return expected function name", RUV_ADD_VAR, errorStatement.getAdditive());
        assertEquals("Should return expected function name", RUV_PROP_VAR, errorStatement.getProportional());
        assertEquals("Should return expected function name", OUTPUT_VAR, errorStatement.getFunctionName());

        assertEquals("Should return expected function name", StructuralObsErrorType.COMBINED_ERROR_1.getErrorType(), errorStatement.getErrorType());
    }

    @Test
    public void shouldGetParamValueAsScalar() {
        when(propArg.getSymbRef()).thenReturn(null);

        doReturn(val).when(scalarVal).getValue();
        doReturn(scalarVal).when(propArg).getScalar();

        when(val.getValue()).thenReturn(value);

        errorStatement = new StructuralObsErrorStatement(functionCallType, output, epsilonVar, true);

        assertNotNull("Function name should not be null", errorStatement.getFunctionName());

        assertEquals("Should return expected function name", RUV_ADD_VAR, errorStatement.getAdditive());
        assertEquals("Should return expected function name", "0.0", errorStatement.getProportional());
        assertEquals("Should return expected function name", OUTPUT_VAR, errorStatement.getFunctionName());

        assertEquals("Should return expected function name", StructuralObsErrorType.COMBINED_ERROR_1.getErrorType(), errorStatement.getErrorType());
    }

}
