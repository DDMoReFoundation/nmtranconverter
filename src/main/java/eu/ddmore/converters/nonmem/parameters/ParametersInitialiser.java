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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import crx.converter.engine.FixedParameter;
import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.steps.EstimationStep;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class initialises population parameters and related information details.
 */
public class ParametersInitialiser {

    private final Map<String, PopulationParameter> populationParams = new HashMap<String, PopulationParameter>();
    private final Map<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();
    private final List<ParameterEstimate> parametersToEstimate = new ArrayList<>();
    private final List<FixedParameter> fixedParameters = new ArrayList<>();

    public ParametersInitialiser(List<PopulationParameter> populationParameters, ScriptDefinition scriptDefinition) {
        Preconditions.checkNotNull(populationParameters, "Population Parameters cannot be null");
        Preconditions.checkNotNull(scriptDefinition, "Script Definition cannot be null");
        initialise(populationParameters, scriptDefinition);
    }

    private void initialise(List<PopulationParameter> populationParameters, ScriptDefinition scriptDefinition){

        if (populationParameters==null || populationParameters.isEmpty()) {
            return;
        }else {
            initialisePopulationParams(populationParameters);
        }

        final EstimationStep estimationStep = ScriptDefinitionAccessor.getEstimationStep(scriptDefinition);
        parametersToEstimate.addAll((estimationStep.hasParametersToEstimate())?estimationStep.getParametersToEstimate(): new ArrayList<ParameterEstimate>());
        fixedParameters.addAll((estimationStep.hasFixedParameters())?estimationStep.getFixedParameters(): new ArrayList<FixedParameter>());

        // Find any bounds and initial estimates
        setAllParameterBounds(parametersToEstimate);
    }

    private void initialisePopulationParams(List<PopulationParameter> populationParameters) {

        for (PopulationParameter populationParam : populationParameters) {
            Parameter popParam = new Parameter(populationParam.getSymbId());
            popParam.setPopParameter(populationParam);
            popParam.setAssignment(populationParam.getAssign()!=null);

            parameters.put(populationParam.getSymbId(), popParam);
        }
    }

    /**
     * This method sets all maps for lower and upper bounds as well as for initial estimates from parameters to estimate.
     *  
     * @param parametersToEstimate
     */
    private void setAllParameterBounds(List<ParameterEstimate> parametersToEstimate) {
        for (ParameterEstimate paramEstimate : parametersToEstimate) {
            setParameterBounds(paramEstimate);
            populationParams.remove(paramEstimate.getSymbRef().getSymbIdRef());
        }
        for (FixedParameter fixedParameter : fixedParameters){
            setParameterBounds(fixedParameter.pe);
            populationParams.remove(fixedParameter.pe.getSymbRef().getSymbIdRef());
        }
    }

    private void setParameterBounds(ParameterEstimate paramEstimate){
        String symbolId = paramEstimate.getSymbRef().getSymbIdRef();
        Parameter param = parameters.get(symbolId);
        if(param != null){
            param.setInitialEstimate(paramEstimate.getInitialEstimate());
            param.setLowerBound(paramEstimate.getLowerBound());
            param.setUpperBound(paramEstimate.getUpperBound());
        }
    }

    /**
     * Get omega object from random variable provided.
     * This will set omega symb id as well as bounds if there are any.
     * 
     * @param randomVar
     * @return
     */
    public OmegaParameter createOmegaFromRandomVarName(String omegaSymbId) {
        OmegaParameter omegaParameter = null;
        if(omegaSymbId!= null){
            omegaParameter = new OmegaParameter(omegaSymbId);
            Parameter param = parameters.get(omegaSymbId);
            omegaParameter.setInitialEstimate(param.getInitialEstimate());

            if(param.getInitialEstimate() == null){
                Rhs scalar = param.getPopParameter().getAssign();
                if(scalar!=null){
                    omegaParameter.setInitialEstimate(scalar);
                    omegaParameter.setFixed(true);
                }
                parameters.remove(omegaSymbId);
                populationParams.remove(omegaSymbId);
            }
        }
        return omegaParameter;
    }

    public List<ParameterEstimate> getParametersToEstimate() {
        return parametersToEstimate;
    }

    public List<FixedParameter> getFixedParameters() {
        return fixedParameters;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public Map<String, PopulationParameter> getPopulationParams() {
        return populationParams;
    }
}
