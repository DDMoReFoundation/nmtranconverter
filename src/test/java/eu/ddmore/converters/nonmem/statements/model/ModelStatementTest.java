/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;

/**
 * Junit tests for ModelStatement class.
 */
@PrepareForTest(ModelStatement.class)
public class ModelStatementTest extends BasicTestSetup {

    private static final String MODEL_STMT_FOR_COUNT_DATA = "model statement for : discrete Count data";
    private static final String MODEL_STMT_FOR_TTE = "model statement for : discrete Time To Event";
    private static final String MODEL_STMT_WITH_DERIVATIVE_VAR = "model statement for : derivative variables (DES)";
    private static final String MODEL_STMT_WITH_NON_DERIVATIVE_PRED = "model statement for : non derivative variables (Pred)";

    @Mock DiscreteHandler discreteHandler;
    @Mock DiscreteStatement discreteStatement;
    @Mock ContinuousStatement continuousStatement;
    @Mock NonDerivativePredStatement nonDerivativePredStatement;
    @Mock ModelStatementHelper statementHelper;
    @Mock DerivativeVariable derivativeVariable;
    ModelStatement modelStatement;

    List<DerivativeVariable> derivativeVars = new ArrayList<>();
    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);
        when(context.getDiscreteHandler()).thenReturn(discreteHandler);

        whenNew(DiscreteStatement.class).withArguments(Matchers.any(ModelStatementHelper.class)).thenReturn(discreteStatement);
        whenNew(ContinuousStatement.class).withArguments(Matchers.any(ModelStatementHelper.class)).thenReturn(continuousStatement);
        whenNew(NonDerivativePredStatement.class).withArguments(Matchers.any(ModelStatementHelper.class)).thenReturn(nonDerivativePredStatement);
        when(context.getDerivativeVars()).thenReturn(derivativeVars);
    }

    @Test
    public void shouldGetModelStatementForCountData() {
        when(discreteHandler.isDiscrete()).thenReturn(true);
        when(discreteHandler.isCountData()).thenReturn(true);
        when(discreteStatement.getModelStatementForCountData(Matchers.any(DiscreteHandler.class))).thenReturn(new StringBuilder(MODEL_STMT_FOR_COUNT_DATA));

        modelStatement = new ModelStatement(context);
        StringBuilder statement = modelStatement.getModelStatement();

        assertNotNull("model statement should not be null", statement);
        assertEquals("Should return expected model statement for count data.", MODEL_STMT_FOR_COUNT_DATA.toUpperCase(), statement.toString());
    }

    @Test
    public void shouldGetModelStatementForTimeToEvent() {
        when(discreteHandler.isDiscrete()).thenReturn(true);
        when(discreteHandler.isTimeToEventData()).thenReturn(true);
        when(discreteStatement.getModelStatementForTTE(Matchers.any(DiscreteHandler.class))).thenReturn(new StringBuilder(MODEL_STMT_FOR_TTE));

        modelStatement = new ModelStatement(context);
        StringBuilder statement = modelStatement.getModelStatement();

        assertNotNull("model statement should not be null", statement);
        assertEquals("Should return expected model statement for count data.", MODEL_STMT_FOR_TTE.toUpperCase(), statement.toString());
    }

    @Test
    public void shouldGetModelStatementForDerivatireVars() {
        derivativeVars.add(derivativeVariable);

        when(discreteHandler.isDiscrete()).thenReturn(false);
        when(continuousStatement.getContinuousStatement()).thenReturn(new StringBuilder(MODEL_STMT_WITH_DERIVATIVE_VAR));

        modelStatement = new ModelStatement(context);
        StringBuilder statement = modelStatement.getModelStatement();

        assertNotNull("model statement should not be null", statement);
        assertEquals("Should return expected model statement for count data.", MODEL_STMT_WITH_DERIVATIVE_VAR.toUpperCase(), statement.toString());
    }

    @Test
    public void shouldGetModelStatementForNonDerivativePred() {
        when(discreteHandler.isDiscrete()).thenReturn(false);
        when(nonDerivativePredStatement.getPredStatement()).thenReturn(new StringBuilder(MODEL_STMT_WITH_NON_DERIVATIVE_PRED));

        modelStatement = new ModelStatement(context);
        StringBuilder statement = modelStatement.getModelStatement();

        assertNotNull("model statement should not be null", statement);
        assertEquals("Should return expected model statement for count data.", MODEL_STMT_WITH_NON_DERIVATIVE_PRED.toUpperCase(), statement.toString());
    }
}
