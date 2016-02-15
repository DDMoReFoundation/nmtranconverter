package eu.ddmore.converters.nonmem.statements.model;

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
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.engine.parts.BaseStep.MultipleDvRef;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.ErrorStatementEmitter;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

@PrepareForTest({ErrorStatementHandler.class, ScriptDefinitionAccessor.class})
public class ErrorStatementHandlerTest extends BasicTestSetup {

    private static final String ERROR_STATEMENT_CONTENT = Formatter.endline()+"error";
    private static final String varDefStatementString = new String("K = (CL/V)");
    @Mock private ErrorStatement error;
    @Mock private ErrorStatementEmitter errorStatementEmitter;
    @Mock private MultipleDvRef dvReference;
    @Mock private SymbolRef columnName;
    @Mock private DiffEquationStatementBuilder desBuilder;

    private ErrorStatementHandler errorStatementHandler;
    private Map<String, ErrorStatement> errorStatements = new HashMap<String, ErrorStatement>();
    private Map<String, String> varDefs = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        mockStatic(ScriptDefinitionAccessor.class);

        context = mock(ConversionContext.class,RETURNS_DEEP_STUBS);
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);

        errorStatements.put(COL_ID_1, error);
        when(context.getErrorStatements()).thenReturn(errorStatements);

        whenNew(ErrorStatementEmitter.class).withArguments(Matchers.any(ErrorStatement.class)).thenReturn(errorStatementEmitter);
        when(errorStatementEmitter.getErrorStatementDetails()).thenReturn(new StringBuilder(ERROR_STATEMENT_CONTENT));
    }

    @Test
    public void shouldGetErrorStatementWithNoParameters() throws Exception {
        errorStatementHandler = new ErrorStatementHandler(context);
        String errorStatement = errorStatementHandler.getErrorStatement();
        assertNotNull("Error statement should not be null", errorStatement);
        assertEquals("expected Error statement should be returned",ERROR_STATEMENT_CONTENT, errorStatement);
    }

    private void setUpForErrorStatementWithDes(){
        when(desBuilder.getVariableDefinitionsStatement(varDefs)).thenReturn(new StringBuilder(varDefStatementString));
    }

    @Test
    public void shouldGetErrorStatementWithDes(){
        setUpForErrorStatementWithDes();
        errorStatementHandler = new ErrorStatementHandler(context);
        String errorStatement = errorStatementHandler.getErrorStatement(desBuilder);
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
        String errorStatement = errorStatementHandler.getErrorStatement();
        assertNotNull("Error statement should not be null", errorStatement);
        assertEquals("expected Error statement should be returned",ERROR_STATEMENT_CONTENT, errorStatement);
    }
}
