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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.FixedParameter;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class builds theta statement for nmtran file.
 */
public class ThetaStatementBuilder {

    private final Map<String, ThetaParameter> thetaParameters = new LinkedHashMap<String, ThetaParameter>();
    private final Map<Integer, String> thetasToEtaOrder;

    private final ParametersInitialiser parametersInitialiser;
    private final CorrelationHandler correlationHandler;
    private final Map<String, OmegaParameter> omegaStatements;
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
            if(scalar !=null){
                param.setInitialEstimate(scalar);
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
        for(OmegaBlock omegaBlock :omegaBlocks){
            for(Eta eta : omegaBlock.getOmegaBlockEtas()){
                if(StringUtils.isNotEmpty(eta.getOmegaName()) && eta.getOmegaName().equals(paramName)){
                    return true;
                }
            }
        }
        return false;
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
