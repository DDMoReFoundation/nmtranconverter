/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.EstimationStep.FixedParameter;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class helps to build theta statement for nmtran file.
 */
public class ThetaStatementBuilder {

    private final Map<String, ThetaParameter> thetaParameters = new LinkedHashMap<String, ThetaParameter>();
    private final Map<Integer, String> thetasToEtaOrder;

    private final ParametersInitialiser parametersInitialiser;
    private final CorrelationHandler correlationHandler;
    private final LinkedHashMap<String, OmegaParameter> omegaStatements;
    private final List<String> verifiedSigmas;

    public ThetaStatementBuilder(ParametersBuilder parametersBuilder, CorrelationHandler correlationHandler, ParametersInitialiser parametersInitialiser) {
        Preconditions.checkNotNull(parametersBuilder, "Parameters builder can not be null.");
        Preconditions.checkNotNull(correlationHandler, "Correlations handler can not be null.");
        Preconditions.checkNotNull(parametersInitialiser, "Parameters initialiser can not be null.");

        thetasToEtaOrder = parametersBuilder.getThetasToEtaOrder();
        this.parametersInitialiser = parametersInitialiser;
        omegaStatements = parametersBuilder.getOmegasBuilder().getOmegaStatements();
        verifiedSigmas = parametersBuilder.getSigmasBuilder().getVerifiedSigmas();
        this.correlationHandler = correlationHandler;

        initialiseThetaStatement();
    }

    private void initialiseThetaStatement(){
        final Map<String, ThetaParameter> unOrderedThetas = new HashMap<String, ThetaParameter>();

        for(ParameterEstimate parameter : parametersInitialiser.getParametersToEstimate()){
            Preconditions.checkNotNull(parameter.getSymbRef(), "Parameter to estimate doesnt have parameter symbol.");
            String paramName = parameter.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, false);
            parametersInitialiser.getParameters().remove(paramName);
        }
        for(FixedParameter fixedParameter : parametersInitialiser.getFixedParameters()){
            Preconditions.checkNotNull(fixedParameter.pe.getSymbRef(), "Fixed Parameter doesnt have parameter symbol.");
            String paramName = fixedParameter.pe.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);
            parametersInitialiser.getParameters().remove(paramName);
        }
        for(String paramName : parametersInitialiser.getPopulationParams().keySet()){
            Parameter param = parametersInitialiser.getParameters().get(paramName);
            Rhs scalar = param.getPopParameter().getAssign();
            //Rhs scalar = parametersInitialiser.getRhsForSymbol(paramName);
            if(scalar !=null){
                param.setInitialEstimate(scalar);
                //parametersInitialiser.getInitialEstimates().put(paramName, scalar);
                createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);
            }
        }
        thetaParameters.putAll(unOrderedThetas);

    }

    /**
     * Creates thetas for parameter name passed after verifying if its valid theta and adds it to ordered thetas map.
     * 
     * @param unOrderedThetas
     * @param paramName
     * @param isFixed
     */
    private void createAndAddThetaForValidParamToMap(final Map<String, ThetaParameter> unOrderedThetas, String paramName, Boolean isFixed) {
        if(validateParamName(paramName)){
            ThetaParameter thetaParameter = new ThetaParameter(paramName);
            if(!thetasToEtaOrder.containsValue(paramName)){
                addThetaToMap(unOrderedThetas, thetaParameter,isFixed);
            }
            addThetaToMap(thetaParameters, thetaParameter,isFixed);
        }
    }

    /**
     * Creates Theta for the parameter name provided and add it to theta statement map 
     * @param unOrderedThetas
     * @param paramName
     * @param isFixed
     */
    private void addThetaToMap(final Map<String, ThetaParameter> unOrderedThetas, ThetaParameter thetaParameter, Boolean isFixed) {
        String paramName = thetaParameter.getSymbId();
        Parameter param = parametersInitialiser.getParameters().get(paramName);
        if(param.getInitialEstimate()==null && param.getLowerBound()==null && param.getUpperBound()==null ){
            return;
        }else{
            thetaParameter.setParameterBounds(param.getInitialEstimate(),param.getLowerBound(),param.getUpperBound());
        }
        if(isFixed!=null) thetaParameter.setFixed(isFixed);
        unOrderedThetas.put(paramName, thetaParameter);
    }

    /**
     * Validate parameter before adding it to Theta, by checking if it is omega or sigma or already added to theta.
     */
    private boolean validateParamName(String paramName) {
        boolean isValid = true;
        if(paramName== null ||  omegaStatements.containsKey(paramName) 
                || verifiedSigmas.contains(paramName) || thetaParameters.containsKey(paramName)
                || validateThetaParamForCorrOmegas(paramName, correlationHandler.getOmegaBlocksInIOV()) 
                || validateThetaParamForCorrOmegas(paramName, correlationHandler.getOmegaBlocksInNonIOV())){
            isValid = false;
        }

        return isValid;
    }

    private boolean validateThetaParamForCorrOmegas(String paramName, List<OmegaBlock> omegaBlocks) {
        boolean isValid = false;
        for(OmegaBlock omegaBlock :omegaBlocks){
            isValid = omegaBlock.getEtasToOmegas().values().contains(paramName);
            if(isValid){
                return isValid;
            }
        }
        return isValid;
    }

    /**
     * Prepares theta statement for thetas if present.
     */
    public String getThetaStatementBlock(){
        StringBuilder thetaStatement = new StringBuilder();
        if (!thetaParameters.isEmpty()) {
            thetaStatement.append(Formatter.endline()+Formatter.theta());
            for (String thetaVar : thetaParameters.keySet()) {
                thetaStatement.append(ParameterStatementHandler.addParameter(thetaParameters.get(thetaVar)));
            }
        }
        return thetaStatement.toString();
    }

    public Map<String, ThetaParameter> getThetaParameters() {
        return thetaParameters;
    }
}
