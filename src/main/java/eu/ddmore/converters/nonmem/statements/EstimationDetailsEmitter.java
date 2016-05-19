/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.steps.EstimationStep;

import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.modellingsteps.Algorithm;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty;

/**
 * This class assists creation of estimation statement details.
 */
public class EstimationDetailsEmitter {

    public enum EstConstant{
        DEFAULT_COND_STATEMENT("COND"),
        SAEM_STATEMENT("SAEM AUTO=1 PRINT=100 CINTERVAL=30 ATOL=6 SIGL=6"),
        FOCEI_STATEMENT ("COND INTER NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        FOCE_STATEMENT ("COND NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        FO_STATEMENT ("ZERO NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        TTE_DATA_LAPLACE_OPTION(" LIKELIHOOD LAPLACE NUMERICAL NOINTERACTION"),
        COUNT_DATA_LAPLACE_OPTION(" -2LL LAPLACE NOINTERACTION");

        private String statement;
        EstConstant(String statement){
            this.statement = statement;
        }
        public String getStatement() {
            return statement;
        }
    }

    public enum Method{
        FO, FOCE, FOCEI, SAEM
    }

    private static final String METHOD = "METHOD=";
    private final List<EstimationStep> estimationSteps;
    private Boolean isSAEM = false;

    private final DiscreteHandler discreteHandler;
    private StringBuilder estStatement = new StringBuilder();
    private String covStatement;

    public EstimationDetailsEmitter(ScriptDefinition scriptDefinition, DiscreteHandler discreteHandler){
        this.discreteHandler = discreteHandler;
        estimationSteps = ScriptDefinitionAccessor.getEstimationSteps(scriptDefinition);
        initialise();
    }

    private void initialise(){
        processEstimationStatement();
    }

    /**
     * This method creates estimation statement for nonmem file from estimation steps collected from steps map.
     */
    private void processEstimationStatement() {
        Boolean isCovFound = false;
        estStatement.append(Formatter.endline());

        if(estimationSteps!=null && !estimationSteps.isEmpty()){
            estStatement.append(Formatter.est());
            for(EstimationStep estStep : estimationSteps){
                for(EstimationOperation operationType :estStep.getOperations()){
                    String optType = operationType.getOpType();
                    //If covariate is not found in any other operations or properties
                    if(!isCovFound){
                        isCovFound = checkForCovariateStatement(operationType);
                    }

                    if(EstimationOpType.EST_POP.value().equals(optType)){
                        estStatement.append(buildEstimationStatementFromAlgorithm(operationType.getAlgorithm()));
                    }else if(EstimationOpType.EST_INDIV.value().equals(optType)){
                        break;
                    }
                }
            }
        }
        setCovStatement(isCovFound);
    }

    private StringBuilder buildEstimationStatementFromAlgorithm(Algorithm algorithm) {
        StringBuilder statement = new StringBuilder();

        statement.append(METHOD);
        if (algorithm!=null) {
            String methodDefinition =algorithm.getDefinition().trim().toUpperCase();
            if (methodDefinition.equals(Method.FO.toString())) {
                statement.append(EstConstant.FO_STATEMENT.getStatement());
            }
            else if(methodDefinition.equals(Method.FOCE.toString())) {
                statement.append(EstConstant.FOCE_STATEMENT.getStatement());
                statement.append(appendDiscreteEstOptions());
            }
            else if (methodDefinition.equals(Method.FOCEI.toString())) {
                statement.append(EstConstant.FOCEI_STATEMENT.getStatement());
                statement.append(appendDiscreteEstOptions());
            }
            else if (methodDefinition.equals(Method.SAEM.toString())) {
                isSAEM = true;
                statement.append(EstConstant.SAEM_STATEMENT.getStatement());
                statement.append(appendDiscreteEstOptions());
            }
            else {
                statement.append(methodDefinition);
            }
        } else {
            statement.append(EstConstant.DEFAULT_COND_STATEMENT.getStatement());
        }
        return statement;
    }

    private void setCovStatement(Boolean isCovFound){
        covStatement = (isCovFound) ? Formatter.cov(): "";
    }

    private StringBuilder appendDiscreteEstOptions() {
        StringBuilder statement = new StringBuilder();
        if(discreteHandler.isCountData()){
            statement.append(EstConstant.COUNT_DATA_LAPLACE_OPTION.getStatement());
        }else if(discreteHandler.isTimeToEventData()){
            statement.append(EstConstant.TTE_DATA_LAPLACE_OPTION.getStatement());
        }
        return statement;
    }

    /**
     * This method creates COV statement. 
     * this method is added here, as it is dependent on availability of EstFIM.
     * @param fout
     */
    public String getCovStatement(){
        return Formatter.endline()+covStatement;
    }

    /**
     * This method determines if covariate values will exist, depending upon value specified in equation
     *  
     * @param equation
     * @return
     */
    private Boolean isCovPropertyForEstOperation(Scalar value) {
        return Boolean.parseBoolean(value.valueToString());
    }

    /**
     * Checks if covariate statement exists for estimation operation type and return boolean result.
     * 
     * @param operationType
     * @return boolean result
     */
    private Boolean checkForCovariateStatement(EstimationOperation operationType){
        String optType = operationType.getOpType();
        if(EstimationOpType.EST_FIM.value().equals(optType)){
            return true;
        }else if(EstimationOpType.EST_POP.value().equals(optType)){
            for(OperationProperty property : operationType.getProperty()){
                if(property.getName().equals("cov") && property.getAssign()!=null){
                    if(property.getAssign().getScalar()!=null){
                        return isCovPropertyForEstOperation(property.getAssign().getScalar());
                    }
                    //else if(property.getAssign().getEquation()!=null){
                    //Equation equation = property.getAssign().getEquation();
                    //return isCovPropertyForEstOperation(equation.getScalar().getValue());
                    //}
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
}
