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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import crx.converter.spi.blocks.StructuralBlock;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.InitialCondition;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

public class InitConditionBuilderTest extends BasicTestSetup {

    @Mock StructuralBlock structBlock;
    @Mock DerivativeVariable variableType;
    @Mock InitialCondition initCondition;
    @Mock StandardAssignable initialValueType;
    @Mock SymbolRef symbolRef;
    @Mock Rhs assign;
    @Mock Scalar scalar;
    @Mock IntValue scalarValue;

    InitConditionBuilder initConditionBuilder;
    List<StructuralBlock> structuralBlocks = new ArrayList<>();
    List<DerivativeVariable> variableTypes = new ArrayList<>();
    BigInteger scalarVal =  new BigInteger("1");
    private final String symbol = "SYMBOL";
    private final String expectedInitAmountWithSymbol = "A_0(1) = "+symbol+Formatter.endline()+Formatter.endline();
    private final String expectedInitAmountWithScalar = "A_0(1) = "+scalarVal+Formatter.endline()+Formatter.endline();

    @Before
    public void setUp() throws Exception {
        when(symbolRef.getSymbIdRef()).thenReturn(symbol);
        when(assign.getSymbRef()).thenReturn(symbolRef);

        when(scalar.valueToString()).thenReturn(scalarVal.toString());
        //doReturn(scalarValue).when(scalar).getValue();
        doReturn(scalar).when(assign).getScalar();

        when(initialValueType.getAssign()).thenReturn(assign);
        when(initCondition.getInitialValue()).thenReturn(initialValueType);
        when(variableType.getInitialCondition()).thenReturn(initCondition);

        variableTypes.add(variableType);

        when(structBlock.getStateVariables()).thenReturn(variableTypes);
        structuralBlocks.add(structBlock);
    }

    @Test
    public void ShouldGetDiffInitialConditionsForSymbRef() {
        initConditionBuilder = new InitConditionBuilder();
        StringBuilder outputStatement = initConditionBuilder.getDifferentialInitialConditions(structuralBlocks);
        assertNotNull("Output statement for init condition cannot be null.", outputStatement);
        assertEquals("", expectedInitAmountWithSymbol, outputStatement.toString());
    }

    @Test
    public void ShouldGetDiffInitialConditionsForScalar() {
        when(assign.getSymbRef()).thenReturn(null);
        initConditionBuilder = new InitConditionBuilder();
        StringBuilder outputStatement = initConditionBuilder.getDifferentialInitialConditions(structuralBlocks);
        assertNotNull("Output statement for init condition cannot be null.", outputStatement);
        assertEquals("", expectedInitAmountWithScalar, outputStatement.toString());
    }

}
