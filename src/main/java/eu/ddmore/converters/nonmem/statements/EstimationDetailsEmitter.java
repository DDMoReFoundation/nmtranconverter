/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;

import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.commontypes.BooleanValue;
import eu.ddmore.libpharmml.dom.commontypes.TrueBoolean;
import eu.ddmore.libpharmml.dom.maths.Equation;
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
        FO_STATEMENT ("ZERO NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT");

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

    private final List<EstimationStep> estimationSteps;
    private Boolean isCovFound = false;
    private Boolean isSAEM = false;

    private final DiscreteHandler discreteHandler;
    private StringBuilder estStatement = new StringBuilder();

    public EstimationDetailsEmitter(ScriptDefinition scriptDefinition, DiscreteHandler discreteHandler){
        this.discreteHandler = discreteHandler;
        estimationSteps = ScriptDefinitionAccessor.getEstimationSteps(scriptDefinition);
    }

    public StringBuilder buildEstimationStatementFromAlgorithm(Algorithm algorithm) {
        StringBuilder statement = new StringBuilder();

        statement.append("METHOD=");
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
                statement.append(Formatter.endline(EstConstant.SAEM_STATEMENT.getStatement()));
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

    private StringBuilder appendDiscreteEstOptions() {
        StringBuilder statement = new StringBuilder();
        if(discreteHandler.isCountData()){
            statement.append(" -2LL LAPLACE NOINTERACTION");
        }else if(discreteHandler.isTimeToEventData()){
            statement.append(" LIKELIHOOD LAPLACE NUMERICAL NOINTERACTION");
        }
        return statement;
    }

    /**
     * This method will create estimation statement for nonmem file from estimation steps collected from steps map.
     * 
     * @return estimation statement
     */
    public StringBuilder processEstimationStatement() {
        estStatement.append(Formatter.endline());

        if(estimationSteps!=null && !estimationSteps.isEmpty()){
            estStatement.append(Formatter.est());
            for(EstimationStep estStep : estimationSteps){
                for(EstimationOperation operationType :estStep.getOperations()){
                    String optType = operationType.getOpType();
                    isCovFound = checkForCovariateStatement(operationType);

                    if(EstimationOpType.EST_POP.value().equals(optType)){
                        estStatement.append(buildEstimationStatementFromAlgorithm(operationType.getAlgorithm()));
                    }else if(EstimationOpType.EST_INDIV.value().equals(optType)){
                        break;
                    }
                }
            }
        }

        return estStatement;
    }

    /**
     * This method creates COV statement. 
     * this method is added here, as it is dependent on availability of EstFIM.
     * @param fout
     */
    public String getCovStatement(){
        String covStatement = (isCovFound) ? Formatter.cov(): "";
        return Formatter.endline()+covStatement;
    }

    /**
     * This method determines if covariate values will exist, depending upon value specified in equation
     *  
     * @param equation
     * @return
     */
    private Boolean isCovPropertyForEstOperation(Object value) {
        if(value instanceof BooleanValue){
            BooleanValue val = (BooleanValue) value;
            return (val instanceof TrueBoolean);
        }
        return false;
    }

    /**
     * Checks if covariate statement exists for estimation operation type and return boolean result.
     * 
     * @param operationType
     * @return boolean result
     */
    private Boolean checkForCovariateStatement(EstimationOperation operationType){
        //If covariate is found in any other operations or properties already then return
        if(isCovFound==true){
            return isCovFound;
        }
        String optType = operationType.getOpType();
        if(EstimationOpType.EST_FIM.value().equals(optType)){
            return true;
        }else if(EstimationOpType.EST_POP.value().equals(optType)){
            for(OperationProperty property : operationType.getProperty()){
                if(property.getName().equals("cov") && property.getAssign()!=null){
                    if(property.getAssign().getScalar()!=null){
                        return isCovPropertyForEstOperation(property.getAssign().getScalar().getValue());
                    }else if(property.getAssign().getEquation()!=null){
                        Equation equation = property.getAssign().getEquation();
                        return isCovPropertyForEstOperation(equation.getScalar().getValue());
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
}
