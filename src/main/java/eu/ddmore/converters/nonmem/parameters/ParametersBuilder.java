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
