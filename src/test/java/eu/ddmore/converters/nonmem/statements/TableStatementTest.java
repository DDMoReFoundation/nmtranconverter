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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import crx.converter.spi.blocks.ParameterBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.input.InputColumn;
import eu.ddmore.converters.nonmem.statements.input.InputColumnsProvider;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * Junit tests for table statement class. 
 */
public class TableStatementTest extends BasicTestSetup {

    private static final String LOGTWT = "LOGTWT";
    private static final String TWT = "TWT";
    private static final String ETA_CL = "ETA_CL";
    private static final String CL = "CL";

    @Mock IndividualParameter parameterType;
    @Mock ParameterBlock block;
    @Mock Eta eta;
    @Mock InputColumnsProvider inputColumns;
    @Mock InputColumnsProvider columnsProvider;

    private List<IndividualParameter> indivParamTypes = new ArrayList<>();
    private List<ParameterBlock> blocks = new ArrayList<>();
    private Set<Eta> orderedEtas = new HashSet<>();
    private List<String> contCovTableColumns = new ArrayList<>();
    private List<String> catCovTableColumns = new ArrayList<>();

    private TableStatement statement;

    String expectedTableStatement = Formatter.endline()+
            Formatter.endline("$TABLE  ID TIME PRED RES WRES DV IPRED IRES IWRES Y NOAPPEND NOPRINT FILE=sdtab")+
            Formatter.endline()+Formatter.endline("$TABLE  ID "+CL+" "+ETA_CL +" NOAPPEND NOPRINT FILE=patab")+
            Formatter.endline()+Formatter.endline("$TABLE  ID "+TWT+" NOAPPEND NOPRINT FILE=catab")+
            Formatter.endline()+Formatter.endline("$TABLE  ID "+LOGTWT+" NOAPPEND NOPRINT FILE=cotab");

    private List<InputColumn> columns = new ArrayList<>();
    private InputColumn column = new InputColumn("DV", false, 1, ColumnType.DV);

    @Before
    public void setUp() throws Exception {
        context = mock(ConversionContext.class,RETURNS_DEEP_STUBS);
        columns.add(column);

        when(context.getInputColumnsHandler().getInputColumnsProvider()).thenReturn(columnsProvider);
        when(columnsProvider.getInputHeaders()).thenReturn(columns);
    }

    @Test
    public void shouldGetAllTableStatements() {

        when(parameterType.getSymbId()).thenReturn(CL);
        indivParamTypes.add(parameterType);

        when(block.getIndividualParameters()).thenReturn(indivParamTypes);
        blocks.add(block);

        when(context.getScriptDefinition().getParameterBlocks()).thenReturn(blocks);

        when(eta.getEtaSymbol()).thenReturn(ETA_CL);
        orderedEtas.add(eta);
        when(context.retrieveOrderedEtas()).thenReturn(orderedEtas);

        contCovTableColumns.add(LOGTWT);
        when(context.getInputColumnsHandler().getInputColumnsProvider()).thenReturn(inputColumns);
        when(inputColumns.getContCovTableColumns()).thenReturn(contCovTableColumns);

        catCovTableColumns.add("TWT");
        when(context.getInputColumnsHandler().getInputColumnsProvider()).thenReturn(inputColumns);
        when(inputColumns.getCatCovTableColumns()).thenReturn(catCovTableColumns);

        statement = new TableStatement(context);
        StringBuilder outputTableStatement = statement.getStatements();
        assertNotNull("should return table statement", outputTableStatement);
        assertEquals("Should return sdtab table statement as expected.", expectedTableStatement, outputTableStatement.toString());
    }

    @Test
    public void shouldGetTableStatementWhenNoColumnsAvailable() {
        String expectedDefaultTableStatement = Formatter.endline()+Formatter.endline("$TABLE  ID TIME PRED RES WRES DV IPRED IRES IWRES Y NOAPPEND NOPRINT FILE=sdtab");

        statement = new TableStatement(context);
        assertNotNull("should return table statement", statement.getStatements());
        assertEquals("Should return sdtab table statement as expected.", expectedDefaultTableStatement, statement.getStatements().toString());
    }
}
