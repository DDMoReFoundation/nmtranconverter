package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.when;

import eu.ddmore.converters.nonmem.statements.ErrorStatement.FunctionArg;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ScalarValueHandler.class)
public class ErrorStatementTest extends BasicTestSetup {

    private static final String OUTPUT_VAR = "CC";
    private static final String RUV_PROP_VAR = "RUV_PROP";
    private static final String RUV_ADD_VAR = "RUV_ADD";

    @Mock FunctionCallType functionCallType;

    FunctionArgument additiveArg;
    @Mock FunctionArgument propArg;
    @Mock FunctionArgument functionArg;

    @Mock JAXBElement<IntValue> scalarVal;
    @Mock JAXBElement<IntValue> val;

    @Mock SymbolRef symbolRef;

    String output = "outputVariablePlaceHolder";
    ErrorStatement errorStatement;
    List<FunctionArgument> args = new ArrayList<FunctionArgument>();

    @Before
    public void setUp() throws Exception {
        when(symbolRef.getSymbIdRef()).thenReturn(ErrorType.COMBINED_ERROR_1.getErrorType());
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

        when(functionCallType.getFunctionArgument()).thenReturn(args);
    }

    @Test
    public void shouldGetFunctionArgumentDetails() {

        errorStatement = new ErrorStatement(functionCallType, output);

        assertNotNull("Function name should not be null", errorStatement.getFunctionName());

        assertEquals("Should return expected function name", RUV_ADD_VAR, errorStatement.getAdditive());
        assertEquals("Should return expected function name", RUV_PROP_VAR, errorStatement.getProportional());
        assertEquals("Should return expected function name", OUTPUT_VAR, errorStatement.getFunctionName());

        assertEquals("Should return expected function name", ErrorType.COMBINED_ERROR_1.getErrorType(), errorStatement.getErrorType());
    }

    @Ignore("Not working as there are issues while mocking ScalarValueHandler.getValue()")
    @Test
    public void shouldGetParamValueAsScalar() {
        when(propArg.getSymbRef()).thenReturn(null);

        IntValue value = new IntValue(1);

        when(val.getValue()).thenReturn(value);
        doReturn(scalarVal).when(propArg).getScalar();
        doReturn(val).when(scalarVal).getValue();

        errorStatement = new ErrorStatement(functionCallType, output);

        assertNotNull("Function name should not be null", errorStatement.getFunctionName());

        assertEquals("Should return expected function name", RUV_ADD_VAR, errorStatement.getAdditive());
        assertEquals("Should return expected function name", RUV_PROP_VAR, errorStatement.getProportional());
        assertEquals("Should return expected function name", OUTPUT_VAR, errorStatement.getFunctionName());

        assertEquals("Should return expected function name", ErrorType.COMBINED_ERROR_1.getErrorType(), errorStatement.getErrorType());
    }

}
