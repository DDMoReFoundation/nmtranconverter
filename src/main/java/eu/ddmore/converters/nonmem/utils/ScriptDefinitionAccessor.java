/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.BaseStep.MultipleDvRef;
import crx.converter.engine.parts.ConditionalDoseEvent;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.ObservationBlock;
import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.Part;
import crx.converter.engine.parts.TemporalDoseEvent;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError.ResidualError;;

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
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");
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
     * @return list of estimation steps
     */
    public static List<EstimationStep> getEstimationSteps(ScriptDefinition scriptDefinition) {
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");
        List<EstimationStep> estSteps = new ArrayList<EstimationStep>(); 
        for(Part nextStep : scriptDefinition.getStepsMap().values()) {
            if (nextStep instanceof EstimationStep){
                estSteps.add((EstimationStep) nextStep);
            }
        }
        return estSteps;
    }

    /**
     * Gets all conditional dose events from all the estimation steps.
     * 
     * @param scriptDefinition
     * @return list of estimation steps
     */
    public static List<ConditionalDoseEvent> getAllConditionalDoseEvents(ScriptDefinition scriptDefinition){
        List<ConditionalDoseEvent> conditionalDoseEvents = new ArrayList<ConditionalDoseEvent>();
        for(EstimationStep estStep : getEstimationSteps(scriptDefinition)){
            conditionalDoseEvents.addAll(estStep.getConditionalDoseEvents());
        }

        return conditionalDoseEvents;
    }

    /**
     * Gets all conditional dose events associated with TIME/IDV column type from all the estimation steps.
     * This support is needed with respect to common converter changes.
     * 
     * @param scriptDefinition
     * @return list of estimation steps
     */
    public static List<TemporalDoseEvent> getAllTemporalDoseEvent(ScriptDefinition scriptDefinition){
        List<TemporalDoseEvent> conditionalDoseEvents = new ArrayList<TemporalDoseEvent>();
        for(EstimationStep estStep : getEstimationSteps(scriptDefinition)){
            if(estStep.getTemporalDoseEvent()!=null){
                conditionalDoseEvents.add(estStep.getTemporalDoseEvent());
            }
        }

        return conditionalDoseEvents;
    }

    /**
     * Gets all multiple dv references from estimation steps.
     * 
     * @param scriptDefinition
     * @return list of multiple dv references.
     */
    public static List<MultipleDvRef> getAllMultipleDvReferences(ScriptDefinition scriptDefinition){
        List<MultipleDvRef> multipleDvRefs = new ArrayList<MultipleDvRef>();
        for(EstimationStep estStep : getEstimationSteps(scriptDefinition)){
            multipleDvRefs.addAll(estStep.getMultipleDvRefs());
        }

        return multipleDvRefs;
    }

    /**
     * Retrieve epsilon random variables with help of script definition.
     * 
     * @param scriptDefinition
     * @return epsilon random variables
     */
    public static Set<ParameterRandomVariable> getEpsilonRandomVariables(ScriptDefinition scriptDefinition) {
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");
        Set<ParameterRandomVariable> epsilonRandomVariables = new HashSet<ParameterRandomVariable>();
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
            if(errorType instanceof StructuredObsError){
                StructuredObsError error = (StructuredObsError) errorType;
                if(error.getResidualError()!=null){
                    residualErrors.add(error.getResidualError());
                }
            }
        }
        return residualErrors;
    }
}
