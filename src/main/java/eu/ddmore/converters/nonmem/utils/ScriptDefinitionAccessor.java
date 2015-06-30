/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError.ResidualError;

/**
 * This class contains methods which access script definition and return expected values/information in return.
 *  
 */
public class ScriptDefinitionAccessor {

    /**
     * This method returns first estimation step found in steps map from script definition.
     * 
     * @param scriptDefinition
     * @return
     */
    public static EstimationStep getEstimationStep(ScriptDefinition scriptDefinition) {
        EstimationStep step = null;
        for (Part nextStep : scriptDefinition.getStepsMap().values()) {
            if (nextStep instanceof EstimationStep) {
                step = (EstimationStep) nextStep;
                break;
            }
        }
        return step;
    }

    /**
     * Get all estimation steps in steps map from script definition.
     * 
     * @param scriptDefinition
     * @return
     */
    public static List<EstimationStep> filterOutEstimationSteps(ScriptDefinition scriptDefinition) {
        List<EstimationStep> estSteps = new ArrayList<EstimationStep>(); 
        for(Part nextStep : scriptDefinition.getStepsMap().values()) {
            if (nextStep instanceof EstimationStep){
                estSteps.add((EstimationStep) nextStep);
            }
        }
        return estSteps;
    }

    /**
     * Retrieve epsilon random variables with help of script definition.
     * 
     * @param scriptDefinition
     * @return
     */
    public static Set<ParameterRandomVariable> getEpsilonRandomVariables(ScriptDefinition scriptDefinition) {
        Set<ParameterRandomVariable> epsilonRandomVariables = new HashSet<>();
        List<ResidualError> residualErrors = retrieveResidualErrors(scriptDefinition);

        for(ParameterBlock paramBlock: scriptDefinition.getParameterBlocks()){
            if(residualErrors.isEmpty() || paramBlock.getRandomVariables().isEmpty()){
                break;
            }
            for(ResidualError error : residualErrors){
                String errorName = error.getSymbRef().getSymbIdRef();
                for(ParameterRandomVariable randomVar : paramBlock.getRandomVariables()){
                    if(randomVar.getSymbId().equals(errorName)){
                        epsilonRandomVariables.add(randomVar);
                    }
                }
            }
        }
        return epsilonRandomVariables;
    }

    /**
     * Retrieves residual error using observation blocks from script definition. 
     * 
     * @param scriptDefinition
     * @return
     */
    private static List<ResidualError> retrieveResidualErrors(ScriptDefinition scriptDefinition){
        List<ResidualError> residualErrors = new ArrayList<>() ;
        for(ObservationBlock block : scriptDefinition.getObservationBlocks()){
            ObservationError errorType = block.getObservationError();
            if(errorType instanceof GaussianObsError){
                GaussianObsError error = (GaussianObsError) errorType;
                if(error.getResidualError()!=null){
                    residualErrors.add(error.getResidualError());
                }
            }
        }
        return residualErrors;
    }
}
