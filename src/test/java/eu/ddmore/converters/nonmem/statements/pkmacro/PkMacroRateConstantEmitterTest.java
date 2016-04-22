/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroRateConstantEmitter.NmtranRateConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro.Arg;

/**
 * Test for pk macro rate constant emitter class.
 */
public class PkMacroRateConstantEmitterTest extends BasicTestSetup {

    @Mock PkMacroDetails details;
    @Mock MacroValue macroValueK12;
    @Mock MacroValue macroValueK21;

    @Mock MacroValue macroValueK13;
    @Mock MacroValue macroValueK31;

    @Mock MacroValue cmtMacroValue;
    @Mock Rhs rhs;
    @Mock Scalar Scalar;

    @Mock PeripheralMacro peripheralMacro;
    @Mock CompartmentMacro cmtMacro;
    @Mock LocalParserHelper parserHelper;

    PkMacroRateConstantEmitter rateConstantEmitter;

    private List<CompartmentMacro> cmtMacros = new  ArrayList<CompartmentMacro>();
    private List<PeripheralMacro> peripheralMacros = new  ArrayList<PeripheralMacro>();
    List<MacroValue> peripheralMacroValues = new ArrayList<MacroValue>();
    List<MacroValue> cmtMacroValues = new ArrayList<MacroValue>();

    String expectedOutputForAdvan4 = Formatter.endline("K23 = "+NmtranRateConstant.K23)+
            Formatter.endline("K32 = "+NmtranRateConstant.K23);
    String expectedOutputForAdvan12 = expectedOutputForAdvan4 + Formatter.endline("K24 = "+ NmtranRateConstant.K23)+
            Formatter.endline("K42 = "+NmtranRateConstant.K23);

    @Before
    public void setUp() throws Exception {
        //parsed rhs symbol. It doesn't matter what the symbol is here. lhs, its order and expected output returned is important. 
        when(parserHelper.getParsedValueForRhs(Matchers.any(Rhs.class)))
        .thenReturn(NmtranRateConstant.K23.toString());

        //compartment value from "Compartment macro" its same for everywhere and used as reference point
        when(Scalar.valueToString()).thenReturn("1");
        when(rhs.getScalar()).thenReturn(Scalar);
        when(cmtMacroValue.getAssign()).thenReturn(rhs);
        when(cmtMacroValue.getArgument()).thenReturn(Arg.CMT.toString());
        cmtMacroValues.add(cmtMacroValue);

        when(context.getLocalParserHelper()).thenReturn(parserHelper);

        when(peripheralMacro.getListOfValue()).thenReturn(peripheralMacroValues);
        peripheralMacros.add(peripheralMacro);
        when(details.getPeripherals()).thenReturn(peripheralMacros);

        when(cmtMacro.getListOfValue()).thenReturn(cmtMacroValues);
        cmtMacros.add(cmtMacro);
        when(details.getCompartments()).thenReturn(cmtMacros);

    }

    //tests result pattern for Advan3 Advan4 
    @Test
    public void shouldGetRateconstantDefinition() {
        when(details.getMacroAdvanType()).thenReturn(PkMacroAnalyser.AdvanType.ADVAN4);
        when(macroValueK12.getArgument()).thenReturn("k_1_2");
        when(macroValueK21.getArgument()).thenReturn("k_2_1");

        peripheralMacroValues.add(macroValueK12);
        peripheralMacroValues.add(macroValueK21);

        rateConstantEmitter = new PkMacroRateConstantEmitter(context, details);
        StringBuilder rateconstantDefinition = rateConstantEmitter.getRateConstDefinitions(); 
        assertNotNull("Should get rate constant definition.", rateconstantDefinition);
        assertEquals("Should return expected output", expectedOutputForAdvan4, rateconstantDefinition.toString());
    }

    //tests result pattern for Advan11 Advan12
    @Test
    public void shouldGetRateconstantDefinitionForAdvan12() {
        when(details.getMacroAdvanType()).thenReturn(PkMacroAnalyser.AdvanType.ADVAN12);
        when(macroValueK12.getArgument()).thenReturn("k_1_2");
        when(macroValueK21.getArgument()).thenReturn("k_2_1");
        when(macroValueK13.getArgument()).thenReturn("k_1_3");
        when(macroValueK31.getArgument()).thenReturn("k_3_1");

        peripheralMacroValues.add(macroValueK12);
        peripheralMacroValues.add(macroValueK21);
        peripheralMacroValues.add(macroValueK13);
        peripheralMacroValues.add(macroValueK31);

        rateConstantEmitter = new PkMacroRateConstantEmitter(context, details);
        StringBuilder rateConstantDefinition = rateConstantEmitter.getRateConstDefinitions(); 
        assertNotNull("Should get rate constant definition.", rateConstantDefinition);
        assertEquals("Should return expected output", expectedOutputForAdvan12, rateConstantDefinition.toString());
    }
}
