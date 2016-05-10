/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VariabilityLevel;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Test Helper class which creates dummy objects and mocks for omega block related tests.
 */
@PrepareForTest(RandomVariableHelper.class)
public class ParametersMockHelper extends BasicTestSetup {

    @Mock ParametersInitialiser paramInitialiser;
    @Mock InterOccVariabilityHandler iovHandler;

    @Mock OmegaBlock omegaBlock;
    @Mock CorrelationHandler correlationHandler;
    @Mock OmegaParameter firstOmegaStatement;
    @Mock OmegaParameter secondOmegaStatement;
    @Mock OmegaParameter coeffOmegaStatement;

    @Mock CorrelationsWrapper correlation;
    @Mock ParameterRandomVariable firstPrv;
    @Mock ParameterRandomVariable secondPrv;

    protected Eta firstEta;
    protected Eta secondEta;

    final String firstEtaSymbol ="ETA_CL";
    final String secondEtaSymbol ="ETA_V";

    final String firstOmegaSymbol = "PPV_CL";
    final String secondOmegaSymbol = "PPV_V";

    final String expectedOmegaTitle = new String("$OMEGA BLOCK(2)");
    List<OmegaParameter> omegas;
    Set<Eta> etas;

    Set<ParameterRandomVariable> epsilonVars;

    protected void createDummyOmegaBlockToPopulate(){

        initialiseDummyEtasInCorrelation();
        initialiseDummyOmegaBlock();
        mockRandomVarHelper();
        mockCorrelationWrapper();

        when(firstOmegaStatement.getSymbId()).thenReturn(firstOmegaSymbol);

        when(paramInitialiser.createOmegaFromRandomVarName(Matchers.anyString())).thenReturn(firstOmegaStatement);
        omegaBlock.addToCorrelations(correlation);
    }

    protected void initialiseDummyOmegaBlock() {
        omegaBlock = new OmegaBlock();

        omegas = new ArrayList<>();
        omegas.add(firstOmegaStatement);
        omegas.add(secondOmegaStatement);

        omegaBlock.addToOrderedEtas(firstEta);
        omegaBlock.addToOrderedEtas(secondEta);

        omegaBlock.addToEtaToOmegas(firstEta, firstOmegaSymbol);
        omegaBlock.addToEtaToOmegas(secondEta, secondOmegaSymbol);
    }

    protected void initialiseDummyEtasInCorrelation() {
        firstEta = new Eta(firstEtaSymbol);
        firstEta.setCorrelationRelated(true);
        firstEta.setOmegaName(firstOmegaSymbol);
        firstEta.setOrder(3);
        firstEta.setOrderInCorr(1);
        firstEta.setVarLevel(VariabilityLevel.IIV);

        secondEta = new Eta(secondEtaSymbol);
        secondEta.setCorrelationRelated(true);
        secondEta.setOmegaName(secondOmegaSymbol);
        secondEta.setOrder(4);
        secondEta.setOrderInCorr(2);
        secondEta.setVarLevel(VariabilityLevel.IIV);
    }

    protected void mockCorrelationWrapper() {
        when(firstPrv.getSymbId()).thenReturn(firstEtaSymbol);
        when(secondPrv.getSymbId()).thenReturn(secondEtaSymbol);

        when(correlation.getFirstParamRandomVariable()).thenReturn(firstPrv);
        when(correlation.getSecondParamRandomVariable()).thenReturn(secondPrv);
    }

    protected void mockRandomVarHelper() {
        mockStatic(RandomVariableHelper.class);

        PositiveRealValueType valueType = Mockito.mock(PositiveRealValueType.class, RETURNS_DEEP_STUBS);
        when(valueType.getVar().getVarId()).thenReturn("1");
        when(RandomVariableHelper.getDistributionTypeStdDev(Matchers.any(ParameterRandomVariable.class))).thenReturn(valueType);
    }

}
