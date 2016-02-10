package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.engine.parts.EstimationStep;
import eu.ddmore.converters.nonmem.statements.EstimationDetailsEmitter.EstConstant;
import eu.ddmore.converters.nonmem.statements.EstimationDetailsEmitter.Method;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modellingsteps.Algorithm;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;

@PrepareForTest(ScriptDefinitionAccessor.class)
public class EstimationDetailsEmitterTest extends BasicTestSetup {

    private String method = "METHOD=";
    @Mock DiscreteHandler discreteHandler;

    EstimationDetailsEmitter detailsEmitter;

    List<EstimationStep> estimationSteps;
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
        detailsEmitter.processEstimationStatement();

        String covStatement = detailsEmitter.getCovStatement();
        assertNotNull("Cov statement cannot be null if operation type is estFIM", covStatement);
        assertFalse("Cov statement is expected here.", covStatement.trim().isEmpty());
        assertEquals("Should return correct statement", Formatter.cov().trim(), covStatement.trim().toString());
    }

    @Test
    public void shouldGetEstimationStatementForFO() {

        String methodType = Method.FO.toString();
        String expectedStatement = Formatter.est()+method+EstConstant.FO_STATEMENT.getStatement();

        shouldVerifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForFOCE() {

        String methodType = Method.FOCE.toString();
        String expectedStatement = Formatter.est()+method+EstConstant.FOCE_STATEMENT.getStatement();

        shouldVerifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForFOCEI() {

        String methodType = Method.FOCEI.toString();
        String expectedStatement = Formatter.est()+method+EstConstant.FOCEI_STATEMENT.getStatement();

        shouldVerifyStatement(methodType, expectedStatement);
    }

    @Test
    public void shouldGetEstimationStatementForSAEM() {

        String methodType = Method.SAEM.toString();
        String expectedStatement = Formatter.est()+method+EstConstant.SAEM_STATEMENT.getStatement();

        shouldVerifyStatement(methodType, expectedStatement);
    }

    private void shouldVerifyStatement(String methodType, String expectedStatement){
        when(estOperation.getAlgorithm()).thenReturn(algorithm);
        when(algorithm.getDefinition()).thenReturn(methodType);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        detailsEmitter.processEstimationStatement();

        String generatedStatement = detailsEmitter.getEstimationStatement().toString().trim();

        assertNotNull("estimation statement cannot be null", generatedStatement);
        assertFalse("estimation statement is expected here.", generatedStatement.isEmpty());
        assertEquals("correct statement should be generated for "+methodType, expectedStatement, generatedStatement);
    }

    @Test
    public void shouldAddSimContentForDiscrete() {
        when(discreteHandler.isDiscrete()).thenReturn(true);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        detailsEmitter.processEstimationStatement();

        String simContent = "sim content";
        assertFalse("Should return sim content if discrete.", detailsEmitter.addSimContentForDiscrete(simContent).isEmpty());
    }

    @Test
    public void shouldNotAddSimContentForDiscrete() {
        when(discreteHandler.isDiscrete()).thenReturn(false);
        detailsEmitter = new EstimationDetailsEmitter(scriptDefinition, discreteHandler);
        detailsEmitter.processEstimationStatement();

        String simContent = "sim content";
        assertTrue("Should not return sim content if discrete.", detailsEmitter.addSimContentForDiscrete(simContent).isEmpty());
    }
}
