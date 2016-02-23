/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.EstimationStep.FixedParameter;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class initialises population parameters and related information details.
 */
public class ParametersInitialiser {

    private final Map<String, PopulationParameter> populationParams = new HashMap<String, PopulationParameter>();
    private final Map<String, Parameter> params = new LinkedHashMap<String, Parameter>();
    private List<ParameterEstimate> parametersToEstimate;
    private List<FixedParameter> fixedParameters;

    public ParametersInitialiser(List<PopulationParameter> populationParameters, ScriptDefinition scriptDefinition) {
        initialise(populationParameters, scriptDefinition);
    }

    /**
     * Initialises population parameters with help of script definition.
     * 
     * @param populationParameters
     * @param scriptDefinition
     */
    private void initialise(List<PopulationParameter> populationParameters, ScriptDefinition scriptDefinition){

        if (populationParameters==null || populationParameters.isEmpty()) {
            return;
        }else {
            initialisePopulationParams(populationParameters);
        }

        final EstimationStep estimationStep = ScriptDefinitionAccessor.getEstimationStep(scriptDefinition);
        parametersToEstimate = (estimationStep.hasParametersToEstimate())?estimationStep.getParametersToEstimate(): new ArrayList<ParameterEstimate>();
        fixedParameters = (estimationStep.hasFixedParameters())?estimationStep.getFixedParameters(): new ArrayList<FixedParameter>();

        // Find any bounds and initial estimates
        setAllParameterBounds(parametersToEstimate);
    }

    private void initialisePopulationParams(List<PopulationParameter> populationParameters) {

        for (PopulationParameter populationParam : populationParameters) {
            Parameter popParam = new Parameter(populationParam.getSymbId());
            popParam.setPopParameter(populationParam);
            popParam.setAssignment(populationParam.getAssign()!=null);

            params.put(populationParam.getSymbId(), popParam);
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
        Parameter param = params.get(symbolId);
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
    public OmegaStatement createOmegaFromRandomVarName(String omegaSymbId) {
        OmegaStatement omegaStatement = null;
        if(omegaSymbId!= null){
            omegaStatement = new OmegaStatement(omegaSymbId);
            Parameter param = params.get(omegaSymbId);
            omegaStatement.setInitialEstimate(param.getInitialEstimate());

            if(param.getInitialEstimate() == null){
                Rhs scalar = param.getPopParameter().getAssign();
                if(scalar!=null){
                    omegaStatement.setInitialEstimate(scalar);
                    omegaStatement.setFixed(true);
                }
                params.remove(omegaSymbId);
                populationParams.remove(omegaSymbId);
            }
        }
        return omegaStatement;
    }

    /**
     * Retrieves rhs from parameter assignment with help of symbol id.
     * @param symbId
     * @return
     */
//    public Rhs getRhsForSymbol(String symbId) {
//        Rhs scalar = null;
//
//        if(getPopulationParams().containsKey(symbId)){
//            Parameter param = getPopulationParams().get(symbId);
//            if(param.getPopParameter().getAssign().getScalar()!=null){
//                scalar = createRhs(symbId, param.getPopParameter().getAssign().getScalar());
//            }
//        }
//        return scalar;
//    }

//    /**
//     * This method will create scalar Rhs object for a symbol from the scalar value provided.
//     *  
//     * @param symbol
//     * @param scalar
//     * @return Rhs object
//     */
//    private Rhs createRhs(String symbol,Scalar scalar) {
//        Rhs rhs = new Rhs();
//        rhs.setScalar(scalar);
//        SymbolRef symbRef = new SymbolRef();
//        symbRef.setId(symbol);
//        rhs.setSymbRef(symbRef);
//        return rhs;
//    }

    public List<ParameterEstimate> getParametersToEstimate() {
        return parametersToEstimate;
    }

    public List<FixedParameter> getFixedParameters() {
        return fixedParameters;
    }

    public void setFixedParameters(List<FixedParameter> fixedParameters) {
        this.fixedParameters = fixedParameters;
    }

    public Map<String, Parameter> getParams() {
        return params;
    }

    public void setParametersToEstimate(List<ParameterEstimate> parametersToEstimate) {
        this.parametersToEstimate = parametersToEstimate;
    }

    
    public Map<String, PopulationParameter> getPopulationParams() {
        return populationParams;
    }
}
