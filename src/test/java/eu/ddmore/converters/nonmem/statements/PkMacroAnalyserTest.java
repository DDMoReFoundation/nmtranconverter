package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.OralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

@RunWith(PowerMockRunner.class)
public class PkMacroAnalyserTest {

    PkMacroAnalyser analyser = new PkMacroAnalyser();
    @Mock ConversionContext context;
    @Mock ScriptDefinition definition;
    @Mock PkMacroDetails details;
    @Mock StructuralBlock block;

    private List<CompartmentMacro> cmtMacros = new  ArrayList<CompartmentMacro>();
    private List<EliminationMacro> eliminationMacros = new  ArrayList<EliminationMacro>();
    private List<IVMacro> ivMacros = new  ArrayList<IVMacro>();
    private List<AbsorptionOralMacro> oralMacros = new  ArrayList<AbsorptionOralMacro>();
    private List<PeripheralMacro> peripheralMacros = new  ArrayList<PeripheralMacro>();

    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(definition);
        List<StructuralBlock> blocks = new ArrayList<StructuralBlock>();
        blocks.add(block);
        when(definition.getStructuralBlocks()).thenReturn(blocks);

        when(details.getCompartments()).thenReturn(cmtMacros);
        when(details.getEliminations()).thenReturn(eliminationMacros);
        when(details.getIvs()).thenReturn(ivMacros);
        when(details.getAbsorptionOrals()).thenReturn(oralMacros);
        when(details.getPeripherals()).thenReturn(peripheralMacros);
    }

    @Test
    public void shouldAnalysePkMacrosInConversionContext() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ArrayList<PKMacro> pkMacros = new ArrayList<PKMacro>();
        pkMacros.add(new CompartmentMacro());
        pkMacros.add(new EliminationMacro());

        when(block.getPKMacros()).thenReturn(pkMacros);

        PkMacroDetails macroDetails = analyser.analyse(context);
        assertTrue("pk macro details should not be empty",!macroDetails.isEmpty());
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN1() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN1.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN2() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN2.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN3() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN3.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN4() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN4.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN11() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN11.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN12() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        String capturedAdvanType = analyser.captureAdvanType(details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN12.toString(), capturedAdvanType);
    }

    private void verifyAdvanType(String expectedAdvan, String capturedAdvan) {
        assertFalse("AdvanType should not be empty",capturedAdvan.isEmpty());
        assertTrue("Correct advan type should be returned as : "+expectedAdvan,expectedAdvan.equals(capturedAdvan));
    }
}
