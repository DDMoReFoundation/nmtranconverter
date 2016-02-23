/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.StructuralBlock;

import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.statements.BasicTestSetup;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

@PrepareForTest(ModelStatementHelper.class)
public class ModelStatementHelperTest extends BasicTestSetup  {

    private static final String IDV_EXAMPLE = Formatter.endline("MU_1 = LOG(POP_KA)")+"KA =  EXP(MU_1 +  ETA(1)) ;";
    private static final String DES_EXAMPLE = Formatter.endline("GUT_DES = A(1)")+"CENTRAL_DES = A(2)";
    private static final String VAR_DEF_EXAMPLE = "K = (CL/V)";

    @Mock IndividualDefinitionEmitter individualDefEmitter;
    @Mock IndividualParameter indivParam;
    @Mock ParameterBlock parameterBlock;
    @Mock StructuralBlock structBlock;
    @Mock VariableDefinition variableDefinition;
    @Mock DiffEquationStatementBuilder desBuilder;

    ModelStatementHelper modelStatementHelper;

    @Before
    public void setUp() throws Exception {
        when(context.getScriptDefinition()).thenReturn(scriptDefinition);
    }

    private void mockForIndivParamAssignment() throws Exception{
        List<ParameterBlock> paramBlocks = new ArrayList<>();
        List<IndividualParameter> individualParameters = new ArrayList<>();

        individualParameters.add(indivParam);
        paramBlocks.add(parameterBlock);

        when(parameterBlock.getIndividualParameters()).thenReturn(individualParameters);
        when(scriptDefinition.getParameterBlocks()).thenReturn(paramBlocks);

        whenNew(IndividualDefinitionEmitter.class).withArguments(Matchers.any(ConversionContext.class)).thenReturn(individualDefEmitter);
        when(individualDefEmitter.createIndividualDefinition(Matchers.any(IndividualParameter.class))).thenReturn(IDV_EXAMPLE);
    }

    @Test
    public void shouldGetIndivParamAssignments() throws Exception {
        mockForIndivParamAssignment();
        modelStatementHelper = new ModelStatementHelper(context);
        StringBuilder idv = modelStatementHelper.getAllIndividualParamAssignments();
        assertNotNull("Individual Param Assignments should not be null", idv);
        assertEquals("Should get example individual definition as expected.", IDV_EXAMPLE, idv.toString());
    }

    private void mockForDiffEqStatement() throws Exception{
        whenNew(DiffEquationStatementBuilder.class).withArguments(Matchers.any(ConversionContext.class)).thenReturn(desBuilder);
        when(desBuilder.getDifferentialEquationsStatement()).thenReturn(new StringBuilder(DES_EXAMPLE));
    }

    @Test
    public void shouldGetDiffEquationStatement() throws Exception{
        mockForDiffEqStatement();
        StringBuilder expectedDes = new StringBuilder();
        StringBuilder des = new StringBuilder();
        expectedDes.append(Formatter.des());
        expectedDes.append(DES_EXAMPLE);

        modelStatementHelper = new ModelStatementHelper(context);
        assertNotNull("DES statement cannot be null", modelStatementHelper.getDiffEquationStatement(des));
        assertEquals("Should get expected Diff equation statement.", expectedDes.toString(), des.toString());
    }

    private void mockForVarDefinitionTypesForNonDES() throws Exception{
        List<StructuralBlock> structBlocks = new ArrayList<>();
        List<VariableDefinition> variableDefinitions = new ArrayList<>();
        variableDefinitions.add(variableDefinition);
        structBlocks.add(structBlock);

        when(context.getLocalParserHelper()).thenReturn(localParserHelper);
        when(localParserHelper.parse(Matchers.any())).thenReturn(VAR_DEF_EXAMPLE);
        when(scriptDefinition.getStructuralBlocks()).thenReturn(structBlocks);
        when(structBlock.getLocalVariables()).thenReturn(variableDefinitions);
    }

    @Test
    public void shoudGetVarDefinitionTypesForNonDES() throws Exception{
        mockForVarDefinitionTypesForNonDES();
        modelStatementHelper = new ModelStatementHelper(context);
        StringBuilder varDefinition = modelStatementHelper.getVarDefinitionTypesForNonDES();
        assertNotNull("Var definition types cannot be null", modelStatementHelper.getVarDefinitionTypesForNonDES());
        assertEquals("Should get expected var definition.", VAR_DEF_EXAMPLE, varDefinition.toString());

    }
}
