/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.ParametersBuilder;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;

/**
 * Junit tests for PredCoreStatement class.
 */
public class PredCoreStatementTest extends BasicTestSetup {

    @Mock CovariateDefinition covDef;
    @Mock ContinuousCovariate contCov;
    @Mock CovariateTransformation covTransformation;

    private static final String ETA_VAR = "eta1";
    private static final String THETA_VAR = "Pop_V";
    private PredCoreStatement predCoreStatement;
    private Set<String> thetas = new HashSet<>();
    private Set<Eta> etas= new HashSet<>();
    private List<CovariateDefinition> covDefs = new ArrayList<>();
    private List<CovariateTransformation> covTransformations = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
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
        String etaStatement = "eta1 =  ETA(1)";

        when(context.isSigmaPresent()).thenReturn(true);
        predCoreStatement = new PredCoreStatement(context);

        assertTrue("Pred core statement should contain Eta1", predCoreStatement.getStatement().toString().contains(etaStatement));
    }
}
