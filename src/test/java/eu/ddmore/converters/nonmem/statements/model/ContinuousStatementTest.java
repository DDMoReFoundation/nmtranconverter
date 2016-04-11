/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;

/**
 * Junit tests for ContinuousStatement class. 
 */
public class ContinuousStatementTest extends BasicTestSetup  {

    @Mock ModelStatementHelper modelStatementHelper;
    @Mock DiffEquationStatementBuilder desBuilder;
    @Mock PredCoreStatement predCoreStatement;
    @Mock ErrorStatementHandler errorStatementHandler;

    @Mock DerivativeVariable derivativeVar;

    private Map<String, String> varDefs = new HashMap<String, String>();
    private static final String EXAMPLE_OUTPUT = Formatter.endline()+Formatter.endline("$SUBS ADVAN13 TOL=6")+
            Formatter.endline()+Formatter.endline("$MODEL ")+
            Formatter.endline(COMP1_EXAMPLE)+
            Formatter.endline()+Formatter.endline()+Formatter.endline("$PK ")+
            Formatter.endline("PRED BLOCK")+
            Formatter.endline("MU_1 = LOG(POP_KA)")+Formatter.endline("KA =  EXP(MU_1 +  ETA(1)) ;")+
            Formatter.endline()+Formatter.endline()+
            Formatter.endline("$ERROR ")+Formatter.endline("error");

    ContinuousStatement continuousStatement;

    @Before
    public void setUp() throws Exception {
        mockConversionContext();
        mockModelStatementHelper();
    }

    private void mockConversionContext(){

        context = mock(ConversionContext.class, RETURNS_DEEP_STUBS);

        List<DerivativeVariable> derivativeVars = new ArrayList<>();
        derivativeVars.add(derivativeVar);
        when(derivativeVar.getSymbId()).thenReturn(COL_ID_1);
        when(context.getDerivativeVars()).thenReturn(derivativeVars);

        Map<String, String> varCompartmentSequences = new HashMap<String, String>();
        varCompartmentSequences.put(COL_ID_1, COL_NUM_1.toString());
        when(context.getDerivativeVarCompSequences()).thenReturn(varCompartmentSequences);

        when(context.getEstimationEmitter().isSAEM()).thenReturn(true);
    }

    private void mockModelStatementHelper(){
        when(desBuilder.getVariableDefinitionsStatement(varDefs)).thenReturn(new StringBuilder(VAR_DEF_EXAMPLE));
        when(modelStatementHelper.getContext()).thenReturn(context);

        when(predCoreStatement.getStatement()).thenReturn(new StringBuilder(PRED_EXAMPLE));
        when(modelStatementHelper.getAllIndividualParamAssignments()).thenReturn(new StringBuilder(IDV_EXAMPLE));
        when(modelStatementHelper.getPredCoreStatement()).thenReturn(predCoreStatement);
        when(modelStatementHelper.getVarDefinitionTypesForNonDES()).thenReturn(new StringBuilder(VAR_DEF_EXAMPLE));
        when(modelStatementHelper.getDiffEquationStatement((StringBuilder) Matchers.any())).thenReturn(desBuilder);
        when(modelStatementHelper.getErrorStatementHandler()).thenReturn(errorStatementHandler);
        when(errorStatementHandler.getErrorStatement(desBuilder)).thenReturn(ERROR_EXAMPLE);
    }

    @Test
    public void testGetContinuousStatement() {
        continuousStatement = new ContinuousStatement(modelStatementHelper);
        String contStatement = continuousStatement.getContinuousStatement().toString();

        assertNotNull("Continuous statement should not be null", contStatement);
    }

    @Test
    public void testGetcontinuousStatementWithComponent(){
        continuousStatement = new ContinuousStatement(modelStatementHelper);
        String contStatement = continuousStatement.getContinuousStatement().toString();

        assertNotNull("Continuous statement should not be null", contStatement);
        assertNotNull("Continuous statement should contain component", contStatement.contains(COMP1_EXAMPLE));
        assertEquals("should get expected output",EXAMPLE_OUTPUT,contStatement.toString());
    }

}
