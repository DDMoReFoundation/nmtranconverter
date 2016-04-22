/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

/**
 * Test for pk macro rate constant emitter class.
 */
public class PkMacroRateConstantEmitterTest extends BasicTestSetup {

    @Mock PkMacroDetails details;
    @Mock MacroValue macroValueK12;
    @Mock MacroValue macroValueK21;
    
    PkMacroRateConstantEmitter rateConstantEmitter;
    
    private List<CompartmentMacro> cmtMacros = new  ArrayList<CompartmentMacro>();
    private List<PeripheralMacro> peripheralMacros = new  ArrayList<PeripheralMacro>();
    List<MacroValue> macroValues = new ArrayList<MacroValue>(); 
    
    @Before
    public void setUp() throws Exception {

        when(details.getCompartments()).thenReturn(cmtMacros);
        when(details.getPeripherals()).thenReturn(peripheralMacros);

        when(details.getMacroAdvanType()).thenReturn(PkMacroAnalyser.AdvanType.ADVAN4);
    }

    @Test
    public void shouldGetRateconstantDefinition() {
        peripheralMacros.add(new PeripheralMacro());
        
        rateConstantEmitter = new PkMacroRateConstantEmitter(context, details);
        assertNotNull("Should get rate constant definition.", rateConstantEmitter.getRateConstDefinitions());
    }

}
