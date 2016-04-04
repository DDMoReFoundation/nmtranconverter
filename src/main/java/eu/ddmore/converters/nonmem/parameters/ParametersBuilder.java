/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.Map;
import java.util.Set;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This is class for to build parameter related statements for NmTran conversion.
 */
public class ParametersBuilder {
    private final Map<Integer, String> thetasToEtaOrder;
    private final Set<ParameterRandomVariable> epsilonVars;
    private final ConversionContext context;
    private OmegaStatementBuilder omegasBuilder;
    private SigmaStatementBuilder sigmasBuilder;
    private ThetaStatementBuilder thetasBuilder;

    /**
     * Constructor expects conversion context with script definition which contains all the blocks populated as part of common converter 
     * and parameters initialiser which has all the population parameter related details processed.
     * @param context
     */
    public ParametersBuilder(ConversionContext context){
        this.context = context;
        this.epsilonVars = ScriptDefinitionAccessor.getEpsilonRandomVariables(context.getScriptDefinition());
        this.thetasToEtaOrder = context.getOrderedThetasHandler().getOrderedThetas();
    }

    /**
     * This method initialises all the Sigma, Omega and Theta parameter maps from simple parameters and their properties.
     */
    public void initialiseAllParameters(){

        //need to set omegas and sigma before setting theta params
        omegasBuilder = new OmegaStatementBuilder(context, epsilonVars);
        sigmasBuilder = new SigmaStatementBuilder(context, epsilonVars);
        thetasBuilder = new ThetaStatementBuilder(this, context.getCorrelationHandler(), context.getParameterInitialiser());
    }

    public Map<Integer, String> getThetasToEtaOrder() {
        return thetasToEtaOrder;
    }

    public OmegaStatementBuilder getOmegasBuilder() {
        return omegasBuilder;
    }

    public SigmaStatementBuilder getSigmasBuilder() {
        return sigmasBuilder;
    }

    public ThetaStatementBuilder getThetasBuilder() {
        return thetasBuilder;
    }
}
