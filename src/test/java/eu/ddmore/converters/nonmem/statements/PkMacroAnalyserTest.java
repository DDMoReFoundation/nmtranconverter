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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.OralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PkMacroAnalyser.class)
public class PkMacroAnalyserTest {

    @Mock PkMacroAnalyser analyser;
    @Mock ConversionContext context;
    @Mock ScriptDefinition definition;
    @Mock PkMacroDetails details;

    private List<CompartmentMacro> cmtMacros = new  ArrayList<CompartmentMacro>();
    List<EliminationMacro> eliminationMacros = new  ArrayList<EliminationMacro>();
    List<IVMacro> ivMacros = new  ArrayList<IVMacro>();
    List<OralMacro> oralMacros = new  ArrayList<OralMacro>();
    List<PeripheralMacro> peripheralMacros = new  ArrayList<PeripheralMacro>();

    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(definition);
        when(definition.getStructuralBlocks()).thenReturn(new ArrayList<StructuralBlock>());

        when(details.getCompartments()).thenReturn(cmtMacros);
        when(details.getEliminations()).thenReturn(eliminationMacros);
        when(details.getIvs()).thenReturn(ivMacros);
        when(details.getOrals()).thenReturn(oralMacros);
        when(details.getPeripherals()).thenReturn(peripheralMacros);
    }

    @Test
    public void CreateAnalyserAndGetAdvanType() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());

        when(details.getMacroAdvanType()).thenReturn(PkMacroAnalyser.AdvanType.ADVAN1.toString() );

        when(analyser.analyse(context)).thenReturn(details);
        assertFalse("AdvanType should not be empty",details.getMacroAdvanType().isEmpty());
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN1() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN1.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN2() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN2.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN3() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN3.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN4() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN4.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN11() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);        
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN11.toString(), capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN12() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        analyser = new PkMacroAnalyser();
        String capturedAdvanType = analyser.captureAdvanType(context,details);        
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN12.toString(), capturedAdvanType);
    }

    private void verifyAdvanType(String expectedAdvan, String capturedAdvan) {
        assertFalse("AdvanType should not be empty",capturedAdvan.isEmpty());
        assertTrue("Correct advan type should be returned as : "+expectedAdvan,expectedAdvan.equals(capturedAdvan));
    }
}
