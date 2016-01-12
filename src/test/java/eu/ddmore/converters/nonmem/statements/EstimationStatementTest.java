package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.ddmore.converters.nonmem.utils.Formatter;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class EstimationStatementTest extends BasicTestSetup {

    @Mock EstimationDetailsEmitter estimationEmitter;

    EstimationStatement estStatement;

    String statementExample = "$EST METHOD=SAEM AUTO=1 PRINT=100 CINTERVAL=30 ATOL=6 SIGL=6";
    StringBuilder statementDetail = new StringBuilder(Formatter.endline()+statementExample);
    String simContent = "sim block content";
    String outputStatement = Formatter.endline()+simContent+Formatter.endline()+
            statementExample+Formatter.endline()+
            Formatter.cov()+simContent;

    @Before
    public void setUp() throws Exception {
        when(estimationEmitter.getEstimationStatement()).thenReturn(statementDetail);
        when(estimationEmitter.getCovStatement()).thenReturn(Formatter.endline()+Formatter.cov());
        when(estimationEmitter.addSimContentForDiscrete(Mockito.anyString())).thenReturn(simContent);
    }

    @Test
    public void shouldGetStatementsWithEstimationDetails() {
        estStatement = new EstimationStatement(estimationEmitter);
        String outputEstStatement = estStatement.getStatementsWithEstimationDetails();

        assertNotNull("estimation statement is not null", outputEstStatement);
        assertEquals("Should return expected estimation statement", outputStatement, outputEstStatement);
    }

}
