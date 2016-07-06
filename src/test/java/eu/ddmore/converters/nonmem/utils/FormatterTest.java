package eu.ddmore.converters.nonmem.utils;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;


public class FormatterTest extends BasicTestSetup {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private final String errorMessage = "Should format output as expected";

    @Test
    public void shouldRenameVarForDES() {
        final String expectedOutputVar = "ID_DES";
        String outputVar = Formatter.renameVarForDES(COL_ID_1);
        assertNotNull("Output should not be null.", outputVar);
        assertTrue("Should rename variable as expected", expectedOutputVar.equals(outputVar));
    }

    @Test
    public void shouldGetTimeSymbol() {
        String outputVar = Formatter.getTimeSymbol();
        assertNotNull("Output should not be null.", outputVar);
        assertTrue("Should return time symbol as TIME", ColumnConstant.TIME.toString().equals(outputVar));
        Formatter.setInDesBlock(true);
        outputVar = Formatter.getTimeSymbol();
        assertTrue("Should return time symbol as T", NmConstant.T.toString().equals(outputVar));
    }

    private void verifyTitles(String expectedTitle, String outputTitle, String errorMessage){
        assertNotNull("Title should not be null.", outputTitle);
        assertTrue("Should get title as expected", expectedTitle.equals(outputTitle));
    }

    @Test
    public void shouldGetTitleForTable() {
        final String expectedTitle = "$TABLE ";
        String outputTitle = Formatter.table();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForInput() {
        final String expectedTitle = "$INPUT ";
        String outputTitle = Formatter.input();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForSubs() {
        final String expectedTitle = "$SUBS ";
        String outputTitle = Formatter.subs();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForOmega() {
        final String expectedTitle = "$OMEGA ";
        String outputTitle = Formatter.omega();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForOmegaBlock() {
        final String expectedTitle = "$OMEGA BLOCK(2)";
        String outputTitle = Formatter.omegaBlock(COL_NUM_2);
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForOmegaSameBlock() {
        final String expectedTitle = "$OMEGA BLOCK(2) SAME"+NEW_LINE+"$OMEGA BLOCK(2) SAME"+NEW_LINE;
        String outputTitle = Formatter.omegaSameBlock(COL_NUM_2, COL_NUM_3);
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetDummyEtaStatement() {
        final String expectedTitle = "DUMMY = ETA(1)"+NEW_LINE;
        String outputTitle = Formatter.getDummyEtaStatement();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForEst() {
        final String expectedTitle = "$EST ";
        String outputTitle = Formatter.est();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForProblem() {
        final String expectedTitle = "$TABLE ";
        String outputTitle = Formatter.table();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForSim() {
        final String expectedTitle = "$SIM ";
        String outputTitle = Formatter.sim();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForData() {
        final String expectedTitle = "$DATA ";
        String outputTitle = Formatter.data();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForPk() {
        final String expectedTitle = "$PK "+NEW_LINE;
        String outputTitle = Formatter.pk();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForError() {
        final String expectedTitle = "$TABLE ";
        String outputTitle = Formatter.table();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForDes() {
        final String expectedTitle = "$DES "+NEW_LINE;
        String outputTitle = Formatter.des();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForCov() {
        final String expectedTitle = "$COV ";
        String outputTitle = Formatter.cov();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForSub() {
        final String expectedTitle = "$SUB "+NEW_LINE;
        String outputTitle = Formatter.sub();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForModel() {
        final String expectedTitle = "$MODEL "+NEW_LINE;
        String outputTitle = Formatter.model();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForPred() {
        final String expectedTitle = "$PRED "+NEW_LINE;
        String outputTitle = Formatter.pred();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForAbbr() {
        final String expectedTitle = "$ABBR ";
        String outputTitle = Formatter.abbr();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForTheta() {
        final String expectedTitle = "$THETA "+NEW_LINE;
        String outputTitle = Formatter.theta();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetTitleForSigma() {
        final String expectedTitle = "$SIGMA "+NEW_LINE;
        String outputTitle = Formatter.sigma();
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldGetIndent() {
        final String expectedTitle = "\t"+COL_ID_1;
        String outputTitle = Formatter.indent(COL_ID_1);
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldAddEndlineAfterString() {
        final String expectedTitle = COL_ID_1+NEW_LINE;
        String outputTitle = Formatter.endline(COL_ID_1);
        verifyTitles(expectedTitle, outputTitle, errorMessage);
    }

    @Test
    public void shouldAddEndline() {
        verifyTitles(NEW_LINE, Formatter.endline(), errorMessage);
    }

    @Test
    public void shouldGetFormattedEtaSymbol() {
        final String expectedEta = " ETA("+COL_ID_1+")";
        String outputEta = Formatter.etaFor(String.valueOf(COL_ID_1));
        verifyTitles(expectedEta, outputEta, errorMessage);
    }

    @Test
    public void shouldGetFormattedSymbol() {
        final String expectedSymbol = COL_ID_1;
        String outputSymbol = Formatter.getFormattedSymbol(COL_ID_1.toLowerCase());
        verifyTitles(expectedSymbol, outputSymbol, errorMessage);

    }

    @Test
    public void shouldAddFormattedComment() {
        final String expectedSymbolWithComment = Formatter.indent(Symbol.COMMENT.toString()+COL_ID_1);
        String outputSymbol = Formatter.addComment(COL_ID_1);
        verifyTitles(expectedSymbolWithComment, outputSymbol, errorMessage);
    }

    @Test
    public void shouldGetVarAmountFromCompartment() {
        Map<String,String> derivativeVariableMap = new HashMap<String, String>();
        derivativeVariableMap.put(COL_ID_1, COL_NUM_1.toString());
        final String expectedSymbolWithComment = "A("+COL_NUM_1+")";
        String outputSymbol = Formatter.getVarAmountFromCompartment(COL_ID_1, derivativeVariableMap);
        verifyTitles(expectedSymbolWithComment, outputSymbol, errorMessage);
    }

}
