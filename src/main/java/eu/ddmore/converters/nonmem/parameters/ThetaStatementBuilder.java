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
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.statements.ThetaStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParameterStatementHandler;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class helps to build theta statement for nmtran file.
 */
public class ThetaStatementBuilder {

    private final LinkedHashMap<String, ThetaStatement> thetaStatements = new LinkedHashMap<String, ThetaStatement>();
    private final Map<Integer, String> thetasToEtaOrder;

    private final ParametersInitialiser parameters;
    private final CorrelationHandler correlationHandler;
    private final LinkedHashMap<String, OmegaStatement> omegaStatements;
    private final List<String> verifiedSigmas;

    public ThetaStatementBuilder(ParametersBuilder parametersBuilder, CorrelationHandler correlationHandler, ParametersInitialiser parametersInitialiser) {

        thetasToEtaOrder = parametersBuilder.getThetasToEtaOrder();
        parameters = parametersInitialiser;
        omegaStatements = parametersBuilder.getOmegasBuilder().getOmegaStatements();
        verifiedSigmas = parametersBuilder.getSigmasBuilder().getVerifiedSigmas();
        this.correlationHandler = correlationHandler;

        initialiseThetaStatement();
    }

    private void initialiseThetaStatement(){
        final Map<String, ThetaStatement> unOrderedThetas = new HashMap<String, ThetaStatement>();

        for(ParameterEstimate parameter : parameters.getParametersToEstimate()){
            Preconditions.checkNotNull(parameter.getSymbRef(), "Parameter to estimate doesnt have parameter symbol.");
            String paramName = parameter.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, false);
            parameters.getParams().remove(paramName);
        }
        for(FixedParameter fixedParameter : parameters.getFixedParameters()){
            Preconditions.checkNotNull(fixedParameter.pe.getSymbRef(), "Fixed Parameter doesnt have parameter symbol.");
            String paramName = fixedParameter.pe.getSymbRef().getSymbIdRef();
            createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);
            parameters.getParams().remove(paramName);
        }
        for(String paramName : parameters.getPopulationParams().keySet()){
            Parameter param = parameters.getParams().get(paramName);
            Rhs scalar = param.getPopParameter().getAssign();
            //Rhs scalar = parameters.getRhsForSymbol(paramName);
            if(scalar !=null){
                param.setInitialEstimate(scalar);
                //parameters.getInitialEstimates().put(paramName, scalar);
                createAndAddThetaForValidParamToMap(unOrderedThetas, paramName, true);
            }
        }
        thetaStatements.putAll(unOrderedThetas);

    }

    /**
     * Creates thetas for parameter name passed after verifying if its valid theta and adds it to ordered thetas map.
     *  
     * @param unOrderedThetas
     * @param paramName
     */
    private void createAndAddThetaForValidParamToMap(final Map<String, ThetaStatement> unOrderedThetas, String paramName, Boolean isFixed) {
        if(validateParamName(paramName)){
            ThetaStatement thetaStatement = new ThetaStatement(paramName);
            if(!thetasToEtaOrder.containsValue(paramName)){
                addThetaToMap(unOrderedThetas, thetaStatement,isFixed);
            }
            addThetaToMap(thetaStatements, thetaStatement,isFixed);
        }
    }

    /**
     * Creates Theta for the parameter name provided and add it to theta statement map 
     * @param unOrderedThetas
     * @param paramName
     * @param isFixed
     */
    private void addThetaToMap(final Map<String, ThetaStatement> unOrderedThetas, ThetaStatement thetaStatement, Boolean isFixed) {
        String paramName = thetaStatement.getSymbId();
        Parameter param = parameters.getParams().get(paramName);
        if(param.getInitialEstimate()==null && param.getLowerBound()==null && param.getUpperBound()==null ){
            return;
        }else{
            thetaStatement.setParameterBounds(param.getInitialEstimate(),param.getLowerBound(),param.getUpperBound());
        }
        if(isFixed!=null) thetaStatement.setFixed(isFixed);
        unOrderedThetas.put(paramName, thetaStatement);
    }

    /**
     * Validate parameter before adding it to Theta, by checking if it is omega or sigma or already added to theta.
     * 
     * @param paramName
     * @return
     */
    private boolean validateParamName(String paramName) {
        boolean isValid = true;
        if(paramName== null ||  omegaStatements.containsKey(paramName) 
                || verifiedSigmas.contains(paramName) || thetaStatements.containsKey(paramName)
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
     *  
     * @return omega statement
     */
    public String getThetaStatementBlock(){
        StringBuilder thetaStatement = new StringBuilder();
        if (!thetaStatements.isEmpty()) {
            thetaStatement.append(Formatter.endline()+Formatter.theta());
            for (String thetaVar : thetaStatements.keySet()) {
                thetaStatement.append(ParameterStatementHandler.addParameter(thetaStatements.get(thetaVar)));
            }
        }
        return thetaStatement.toString();
    }

    public LinkedHashMap<String, ThetaStatement> getThetaStatements() {
        return thetaStatements;
    }
}
