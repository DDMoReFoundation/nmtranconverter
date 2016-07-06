/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.spi.steps.EstimationStep;
import eu.ddmore.converters.nonmem.statements.EstimationStatementBuilder.EstConstant;
import eu.ddmore.converters.nonmem.statements.EstimationStatementBuilder.Method;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modellingsteps.Algorithm;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty;

@PrepareForTest(ScriptDefinitionAccessor.class)
public class EstimationDetailsEmitterTest extends BasicTestSetup {

    private static final String NONMEM_OPERATION = "NONMEM";
    private static final String COV_PREFIX = "COV_";
    private static final String METHOD = "METHOD=";
    @Mock DiscreteHandler discreteHandler;
    @Mock CovStatementBuilder covStatementBuilder;

    @Mock OperationProperty algoProperty;
    @Mock OperationProperty firstOpProperty;
    @Mock OperationProperty secondOpProperty;

    EstimationDetailsEmitter detailsEmitter;

    Map<String, String> covOptions = new LinkedHashMap<String, String>();
    List<EstimationStep> estimationSteps;
    List<OperationProperty> operationProperties = new ArrayList<>();
    @Mock EstimationOperation estOperation;
    @Mock Algorithm algorithm;

    @Before
    public void setUp() throws Exception {
        mockStatic(ScriptDefinitionAccessor.class);
        estimationSteps = new ArrayList<EstimationStep>();

        when(estOperation.getOpType()).thenReturn(EstimationOpType.EST_POP.value());
        EstimationOperation[] estOperations = {estOperation};

        when(estStep.getOperations()).thenReturn(estOperations);
        estimationSteps.add(estStep);

        algoProperty = mock(OperationProperty.class,RETURNS_DEEP_STUBS);
        firstOpProperty = mock(OperationProperty.class,RETURNS_DEEP_STUBS);
        secondOpProperty = mock(OperationProperty.class,RETURNS_DEEP_STUBS);

        when(ScriptDefinitionAccessor.getEstimationSteps(scriptDefinition)).thenReturn(estimationSteps);
    }

    @Test
    public void shouldGetCovStatement() {
        estimationSteps = new ArrayList<EstimationStep>();
        when(estOperation.getOpType()).thenReturn(EstimationOpType.EST_FIM.value());
        EstimationOperation[] estOperations = {estOperation};

        when(estStep.getOperations()).thenReturn(estOperations);
        estimationSteps.add(estStep);

        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);

        String covStatement = detailsEmitter.getCovStatement().toString();
        assertNotNull("Cov statement cannot be null if operation type is estFIM", covStatement);
        assertFalse("Cov statement is expected here.", covStatement.trim().isEmpty());
        assertEquals("Should return correct statement", Formatter.cov().trim(), covStatement.trim().toString());
    }

    private void addOptions(){
        when(algoProperty.getName()).thenReturn("algo");
        when(algoProperty.getAssign().getScalar().valueToString()).thenReturn("FO");
        when(firstOpProperty.getAssign().getScalar().valueToString()).thenReturn(COL_NUM_3.toString());
        when(secondOpProperty.getAssign().getScalar().valueToString()).thenReturn(COL_NUM_4.toString());
        operationProperties.add(algoProperty);
        operationProperties.add(firstOpProperty);
        operationProperties.add(secondOpProperty);

        when(estOperation.getProperty()).thenReturn(operationProperties);

        when(estOperation.getOpType()).thenReturn(NONMEM_OPERATION);
    }

    @Test
    public void shouldGetEstimationStatementWithOptions() {
        addOptions();
        when(firstOpProperty.getName()).thenReturn(COL_ID_3);
        when(secondOpProperty.getName()).thenReturn(COL_ID_4);

        String methodType = Method.FO.toString();
        String optionStatement =  " "+COL_ID_3+"="+COL_NUM_3+" "+COL_ID_4+"="+COL_NUM_4;
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FO.getMethod()+" "+optionStatement;
        verifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetCovStatementWithOptions() {
        addOptions();
        when(firstOpProperty.getName()).thenReturn(COV_PREFIX+COL_ID_3);
        when(secondOpProperty.getName()).thenReturn(COV_PREFIX+COL_ID_4);

        covStatementBuilder.buildCovStatement();

        String optionStatement =  COL_ID_3+"="+COL_NUM_3+" "+COL_ID_4+"="+COL_NUM_4;
        String expectedStatement = Formatter.cov()+" "+optionStatement;

        estimationSteps = new ArrayList<EstimationStep>();
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        String covStatement = detailsEmitter.getCovStatement().toString().trim();

        assertNotNull("Cov statement cannot be null if operation type is estFIM", covStatement);
        assertFalse("Cov statement is expected here.", covStatement.isEmpty());
        assertEquals("Should return correct statement", expectedStatement.trim(), covStatement);
    }

    @Test
    public void shouldGetEstimationStatement() {
        String methodType = Method.FO.toString();
        String expectedStatement = Formatter.est()+METHOD+EstConstant.FO.getMethod()+" "+EstConstant.FO.getStatement();
        verifyStatement(methodType, expectedStatement);
    }

    private void verifyStatement(String methodType, String expectedStatement){
        when(estOperation.getAlgorithm()).thenReturn(algorithm);
        when(algorithm.getDefinition()).thenReturn(methodType);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);

        String generatedStatement = detailsEmitter.getEstimationStatement().toString().trim();

        assertNotNull("estimation statement cannot be null", generatedStatement);
        assertFalse("estimation statement is expected here.", generatedStatement.isEmpty());
        assertEquals("correct statement should be generated for "+methodType, expectedStatement, generatedStatement);
    }

    @Test
    public void shouldAddSimContentForDiscrete() {
        when(discreteHandler.isDiscrete()).thenReturn(true);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        String simContent = "sim content";
        assertFalse("Should return sim content if discrete.", detailsEmitter.addSimContentForDiscrete(simContent).isEmpty());
    }

    @Test
    public void shouldNotAddSimContentForDiscrete() {
        when(discreteHandler.isDiscrete()).thenReturn(false);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        String simContent = "sim content";
        assertTrue("Should not return sim content if discrete.", detailsEmitter.addSimContentForDiscrete(simContent).isEmpty());
    }
}
