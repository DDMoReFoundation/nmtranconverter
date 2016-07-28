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
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.engine.ScriptDefinition;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.ParametersBuilder;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * Junit tests for PredCoreStatement class.
 */
@PrepareForTest(ScriptDefinitionAccessor.class)
public class PredCoreStatementTest extends BasicTestSetup {

    @Mock CovariateDefinition covDef;
    @Mock ContinuousCovariate contCov;
    @Mock CovariateTransformation covTransformation;
    @Mock ParameterRandomVariable epsilonVar;

    private static final String ETA_VAR = "eta1";
    private static final String THETA_VAR = "Pop_V";
    private PredCoreStatement predCoreStatement;
    private Set<String> thetas = new HashSet<>();
    private Set<Eta> etas= new HashSet<>();
    private List<CovariateDefinition> covDefs = new ArrayList<>();
    private List<CovariateTransformation> covTransformations = new ArrayList<>();
    private Set<ParameterRandomVariable>  epsilonVars = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        mockStatic(ScriptDefinitionAccessor.class);
        context = mock(ConversionContext.class, RETURNS_DEEP_STUBS);
        parametersHelper = mock(ParametersBuilder.class, RETURNS_DEEP_STUBS);

        Eta eta = new Eta(ETA_VAR);
        eta.setOrder(1);
        etas.add(eta);
        thetas.add(THETA_VAR);
        covDefs.add(covDef);
        covTransformations.add(covTransformation);

        when(context.retrieveOrderedEtas()).thenReturn(etas);
        when(context.getParametersBuilder()).thenReturn(parametersHelper);
        when(context.getLexer().getCovariates()).thenReturn(covDefs);
        when(parametersHelper.getThetasBuilder().getThetaParameters().keySet()).thenReturn(thetas);
        when(contCov.getListOfTransformation()).thenReturn(covTransformations);
        when(covDef.getContinuous()).thenReturn(contCov);

        when(epsilonVar.getSymbId()).thenReturn(COL_ID_3);//WT
        epsilonVars.add(epsilonVar);
        when(ScriptDefinitionAccessor.getEpsilonRandomVariables(Matchers.any(ScriptDefinition.class))).thenReturn(epsilonVars);

        when(context.getParser().getSymbol(Matchers.any())).thenReturn(DROP);
    }

    @Test
    public void shouldGetPredCoreStatement() {
        predCoreStatement = new PredCoreStatement(context);
        assertNotNull("pred core statement should not be null", predCoreStatement.getStatement());
    }

    @Test
    public void shouldGetPredCoreStatementWithDummyEta(){
        String dummyEta = "DUMMY = ETA(1)";
        predCoreStatement = new PredCoreStatement(context);
        assertTrue("Pred core statement should contain dummy eta", predCoreStatement.getStatement().toString().contains(dummyEta));
    }

    @Test
    public void shouldGetPredCoreStatementWithEta(){
        String etaStatement = "eta1 = ETA(1)";

        when(context.isSigmaPresent()).thenReturn(true);
        predCoreStatement = new PredCoreStatement(context);

        assertTrue("Pred core statement should contain Eta1", predCoreStatement.getStatement().toString().contains(etaStatement));
    }
}
