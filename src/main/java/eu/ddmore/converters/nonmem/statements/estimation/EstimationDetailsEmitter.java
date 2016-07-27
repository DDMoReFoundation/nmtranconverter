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
package eu.ddmore.converters.nonmem.statements.estimation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.steps.EstimationStep;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.BooleanValue;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty;

/**
 * This class assists creation of estimation statement details.
 */
public class EstimationDetailsEmitter {

    private static final String NONMEM_OPERATION = "NONMEM";
    private static final String GENERIC_OPERATION = "generic";
    private static final String ALGO_PROP = "algo";
    private static final String TOL_PROP = "TOL";
    private static final String SUB_TOL_PROP = "SUB_TOL";
    private static final String COV_PREFIX = "COV_";

    private final List<EstimationStep> estimationSteps;
    private final DiscreteHandler discreteHandler;

    private final StringBuilder estStatement = new StringBuilder();
    private final CovStatementBuilder covStatementBuilder = new CovStatementBuilder();
    private String tolValue = "";
    private boolean isSAEM = false;

    public EstimationDetailsEmitter(ScriptDefinition scriptDefinition, DiscreteHandler discreteHandler){
        this.discreteHandler = discreteHandler;
        estimationSteps = ScriptDefinitionAccessor.getEstimationSteps(scriptDefinition);
        initialise();
    }

    /**
     * This method creates estimation statement for nonmem file from estimation steps collected from steps map.
     */
    private void initialise() {
        if(estimationSteps!=null && !estimationSteps.isEmpty()){
            for (EstimationStep estStep : estimationSteps){
                propcessEstimationStep(estStep);
            }
        }
        covStatementBuilder.buildCovStatement();
    }

    private void propcessEstimationStep(EstimationStep estStep) {
        String methodName = new String();
        Map<String, String> estOptions = new LinkedHashMap<String, String>();
        for(EstimationOperation operationType :estStep.getOperations()){
            String newMethodName = getMethodNameFromOperation(operationType);
            if(StringUtils.isNotEmpty(newMethodName)){
                methodName = newMethodName;
            }
            if (NONMEM_OPERATION.equals(operationType.getOpType())){
                populateOptions(operationType, estOptions);
            }else {
                if(!covStatementBuilder.isCovFound()){
                    covStatementBuilder.setCovFound(checkForCovariateStatement(operationType));
                }
            }
        }
        EstimationStatementBuilder statementBuilder = new EstimationStatementBuilder(discreteHandler, estOptions);
        estStatement.append(statementBuilder.buildEstimationStatementFromAlgorithm(methodName)+Formatter.endline());
        isSAEM = statementBuilder.isSAEM();
    }

    private void populateOptions(EstimationOperation operationType, Map<String, String> estOptions) {
        for(OperationProperty property : operationType.getProperty()){
            if(property.getAssign()!=null){
                Scalar propertyValue = property.getAssign().getScalar();
                if(property.getName().startsWith(COV_PREFIX)){
                    String propertyName = property.getName().replaceFirst(COV_PREFIX, "");
                    addOptionsFromProperty(propertyName, propertyValue, covStatementBuilder.getCovOptions());
                    covStatementBuilder.setCovFound(true);
                }else if(TOL_PROP.equalsIgnoreCase(property.getName()) 
                        || SUB_TOL_PROP.equalsIgnoreCase(property.getName())){
                    tolValue = propertyValue.valueToString();
                }else if(!ALGO_PROP.equals(property.getName())){
                    addOptionsFromProperty(property.getName(), propertyValue, estOptions);
                }
            }
        }
    }

    private void addOptionsFromProperty(String propertyName, Scalar propertyValue, Map<String, String> options) {
        if(propertyValue!=null){
            if(propertyValue instanceof BooleanValue){
                Boolean isAllowed = getBooleanValueFromScalar(propertyValue);
                if(isAllowed){
                    options.put(propertyName, "");
                }
            }else {
                String optionValue = propertyValue.valueToString();
                options.put(propertyName, optionValue);
            }
        }
    }

    private String getMethodNameFromOperation(EstimationOperation estOperation){
        String optType = estOperation.getOpType();

        if(EstimationOpType.EST_POP.value().equals(optType)){
            return getAlgorithmDefinition(estOperation);
        } else if(GENERIC_OPERATION.equals(optType) || NONMEM_OPERATION.equals(optType)){
            return getMethodFromOperation(estOperation);
        }
        return "";
    }

    private String getAlgorithmDefinition(EstimationOperation operationType) {
        if(operationType.getAlgorithm()==null) {
            return "";
        }
        return operationType.getAlgorithm().getDefinition();
    }

    private String getMethodFromOperation(EstimationOperation operationType) {
        for(OperationProperty property : operationType.getProperty()){
            String methodName = getMethodNameFromProperty(property);
            if(StringUtils.isNotEmpty(methodName)){
                return methodName;
            }
        }
        return "";
    }

    private String getMethodNameFromProperty(OperationProperty property){
        if(ALGO_PROP.equals(property.getName())){
            if(property.getAssign()!=null && property.getAssign().getScalar()!=null){
                return property.getAssign().getScalar().valueToString();
            }
        }
        return "";
    }

    /**
     * This method creates COV statement. 
     * this method is added here, as it is dependent on availability of EstFIM.
     */
    public String getCovStatement(){
        return covStatementBuilder.getCovStatement().toString();
    }

    /**
     * This method determines covariate property value, depending upon value specified in equation
     */
    private Boolean getBooleanValueFromScalar(Scalar value) {
        if(value instanceof BooleanValue){
            return Boolean.parseBoolean(value.valueToString());
        }else {
            return false;
        }
    }

    /**
     * Checks if covariate statement exists for estimation operation type and return boolean result.
     */
    private Boolean checkForCovariateStatement(EstimationOperation operationType){
        String optType = operationType.getOpType();
        if(EstimationOpType.EST_FIM.value().equals(optType)){
            return true;
        }else if(EstimationOpType.EST_POP.value().equals(optType)){
            for(OperationProperty property : operationType.getProperty()){
                if(property.getName().equals("cov") && property.getAssign()!=null){
                    if(property.getAssign().getScalar()!=null){
                        return getBooleanValueFromScalar(property.getAssign().getScalar());
                    }
                }
            };
        }
        return false;
    }

    public StringBuilder getEstimationStatement() {
        return estStatement;
    }

    /**
     * Formats "sim" content associated with estimation for discrete models.
     *  
     * @return sim content if applicable
     */
    public String addSimContentForDiscrete(String simContent){
        return (discreteHandler.isDiscrete())?simContent:"";
    }

    public boolean isSAEM() {
        return isSAEM;
    }

    public String getTolValue() {
        return tolValue;
    }
}