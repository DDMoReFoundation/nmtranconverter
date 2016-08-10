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
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.InitialCondition;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

public class InitConditionBuilderTest extends BasicTestSetup {

    @Mock DerivativeVariable variableType;
    @Mock InitialCondition initCondition;
    @Mock StandardAssignable initialValueType;
    @Mock SymbolRef symbolRef;
    @Mock Rhs assign;
    @Mock Scalar scalar;
    @Mock IntValue scalarValue;

    InitConditionBuilder initConditionBuilder;
    List<DerivativeVariable> variableTypes = new ArrayList<>();
    BigInteger scalarVal =  new BigInteger("1");
    private final String symbol = "SYMBOL";
    private final String expectedInitAmountWithSymbol = "A_0(1) = "+symbol+Formatter.endline()+Formatter.endline();
    private final String expectedInitAmountWithScalar = "A_0(1) = "+scalarVal+Formatter.endline()+Formatter.endline();

    @Before
    public void setUp() throws Exception {
        Map<String, String> varCompartmentSequences = new HashMap<String, String>();
        varCompartmentSequences.put(COL_ID_1, COL_NUM_1.toString());
        when(context.getDerivativeVarCompSequences()).thenReturn(varCompartmentSequences);

        when(symbolRef.getSymbIdRef()).thenReturn(symbol);
        when(assign.getSymbRef()).thenReturn(symbolRef);

        when(scalar.valueToString()).thenReturn(scalarVal.toString());
        //doReturn(scalarValue).when(scalar).getValue();
        doReturn(scalar).when(assign).getScalar();

        when(initialValueType.getAssign()).thenReturn(assign);
        when(initCondition.getInitialValue()).thenReturn(initialValueType);
        when(variableType.getInitialCondition()).thenReturn(initCondition);
        when(variableType.getSymbId()).thenReturn(COL_ID_1);

        variableTypes.add(variableType);

        when(context.getDerivativeVars()).thenReturn(variableTypes);

    }

    @Test
    public void ShouldGetDiffInitialConditionsForSymbRef() {
        initConditionBuilder = new InitConditionBuilder();
        StringBuilder outputStatement = initConditionBuilder.getDifferentialInitialConditions(context);
        assertNotNull("Output statement for init condition cannot be null.", outputStatement);
        assertEquals("", expectedInitAmountWithSymbol, outputStatement.toString());
    }

    @Test
    public void ShouldGetDiffInitialConditionsForScalar() {
        when(assign.getSymbRef()).thenReturn(null);
        initConditionBuilder = new InitConditionBuilder();
        StringBuilder outputStatement = initConditionBuilder.getDifferentialInitialConditions(context);
        assertNotNull("Output statement for init condition cannot be null.", outputStatement);
        assertEquals("", expectedInitAmountWithScalar, outputStatement.toString());
    }

}
