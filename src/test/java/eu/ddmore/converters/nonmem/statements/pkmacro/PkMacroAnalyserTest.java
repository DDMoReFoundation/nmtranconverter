/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import crx.converter.spi.blocks.StructuralBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.AdvanType;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.OralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

/**
 * Junit tests for PkMacroAnalyser class. 
 */
public class PkMacroAnalyserTest extends BasicTestSetup {

    PkMacroAnalyser analyser = new PkMacroAnalyser();
    @Mock ConversionContext context;
    @Mock PkMacroDetails details;
    @Mock StructuralBlock block;

    private List<CompartmentMacro> cmtMacros = new  ArrayList<CompartmentMacro>();
    private List<EliminationMacro> eliminationMacros = new  ArrayList<EliminationMacro>();
    private List<IVMacro> ivMacros = new  ArrayList<IVMacro>();
    private List<AbsorptionOralMacro> oralMacros = new  ArrayList<AbsorptionOralMacro>();
    private List<PeripheralMacro> peripheralMacros = new  ArrayList<PeripheralMacro>();

    @Before
    public void setUp() throws Exception {

        context = mock(ConversionContext.class,RETURNS_DEEP_STUBS);
        
        List<StructuralBlock> blocks = new ArrayList<StructuralBlock>();
        blocks.add(block);
        when(scriptDefinition.getStructuralBlocks()).thenReturn(blocks);

        when(details.getCompartments()).thenReturn(cmtMacros);
        when(details.getEliminations()).thenReturn(eliminationMacros);
        when(details.getIvs()).thenReturn(ivMacros);
        when(details.getAbsorptionOrals()).thenReturn(oralMacros);
        when(details.getPeripherals()).thenReturn(peripheralMacros);
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);
        
        //
        when(context.getInputColumnsHandler().getInputColumnsProvider().isCMTColumnPresent()).thenReturn(false);
        
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

        AdvanType capturedAdvanType = analyser.captureAdvanType(details,AdvanType.ADVAN1.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN1, capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN2() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());

        AdvanType capturedAdvanType = analyser.captureAdvanType(details, AdvanType.ADVAN2.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN2, capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN3() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());

        AdvanType capturedAdvanType = analyser.captureAdvanType(details, AdvanType.ADVAN3.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN3, capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN4() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());

        AdvanType capturedAdvanType = analyser.captureAdvanType(details, AdvanType.ADVAN4.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN4, capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN11() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        ivMacros.add(new IVMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        AdvanType capturedAdvanType = analyser.captureAdvanType(details, AdvanType.ADVAN11.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN11, capturedAdvanType);
    }

    @Test
    public void verifyAdvanTypeCapturedIsADVAN12() {
        cmtMacros.add(new CompartmentMacro());
        eliminationMacros.add(new EliminationMacro());
        oralMacros.add(new OralMacro());
        peripheralMacros.add(new PeripheralMacro());
        peripheralMacros.add(new PeripheralMacro());

        AdvanType capturedAdvanType = analyser.captureAdvanType(details, AdvanType.ADVAN12.getAssociatedDECount());
        verifyAdvanType(PkMacroAnalyser.AdvanType.ADVAN12, capturedAdvanType);
    }

    private void verifyAdvanType(AdvanType expectedAdvan, AdvanType capturedAdvan) {
        assertFalse("AdvanType should not be empty",capturedAdvan.equals(AdvanType.NONE));
        assertTrue("Correct advan type should be returned as : "+expectedAdvan,expectedAdvan.equals(capturedAdvan));
    }
}
