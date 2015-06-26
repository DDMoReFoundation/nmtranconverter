/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.BooleanValue;
import eu.ddmore.libpharmml.dom.commontypes.TrueBoolean;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.modellingsteps.Algorithm;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 *
 */
public class EstimationStatement {
    enum Method{
        FO, FOCE, FOCEI, SAEM
    }

    private List<EstimationStep> estimationSteps = new ArrayList<EstimationStep>();

    private static Boolean covFound = false;
    ConversionContext context;

    public EstimationStatement(ConversionContext convContext){
        this.context = convContext;
        estimationSteps = filterOutEstimationSteps(context.getScriptDefinition());
    }

    /**
     * Compute method for algorithm depending upon its definition.
     * 
     */
    private StringBuilder computeMethod(Algorithm algorithm) {
        StringBuilder estStatement = new StringBuilder();
        String simCommentsForDiscrete = new String(
            Formatter.endline(";Sim_start")
            +Formatter.endline(";$SIM (12345) (12345 UNIFORM) ONLYSIM NOPREDICTION")
            +Formatter.endline(";Sim_end"));

        if(context.getDiscreteHandler().isCountData()){
            estStatement.append(Formatter.endline(simCommentsForDiscrete));
        }
        
        estStatement.append("METHOD=");
        if (algorithm!=null) {
            String methodDefinition =algorithm.getDefinition().trim().toUpperCase();
            if (methodDefinition.equals(Method.FO.toString())) {
                estStatement.append("ZERO MAXEVALS=9999 PRINT=10 NOABORT");
            }
            else if(methodDefinition.equals(Method.FOCE.toString())) {
                estStatement.append("COND MAXEVALS=9999 PRINT=10 NOABORT");
                if(context.getDiscreteHandler().isPoissonDist()){
                    estStatement.append(" -2LL LAPLACE");
                }
            }
            else if (methodDefinition.equals(Method.FOCEI.toString())) {
                estStatement.append("COND INTER MAXEVALS=9999 PRINT=10 NOABORT");
            }
            else if (methodDefinition.equals(Method.SAEM.toString())) {
                estStatement.append("SAEM AUTO=1 PRINT=100"+Formatter.endline());
            }
            else {
                estStatement.append(methodDefinition);
            }
        } else {
            estStatement.append("COND");
        }
        return estStatement;
    }

    /**
     * Collects estimation steps from steps map
     * @param scriptDefinition
     * @return
     */
    public List<EstimationStep> filterOutEstimationSteps(ScriptDefinition scriptDefinition) {
        List<EstimationStep> estSteps = new ArrayList<EstimationStep>(); 
        for(Part nextStep : scriptDefinition.getStepsMap().values()) {
            if (nextStep instanceof EstimationStep){
                estSteps.add((EstimationStep) nextStep);
            }
        }
        return estSteps;
    }

    /**
     * this method will create estimation statement for nonmem file from estimation steps collected from steps map.
     * 
     * @param fout
     */
    public StringBuilder getEstimationStatement() {
        StringBuilder estStatement = new StringBuilder();
        estStatement.append(Formatter.endline());
        estStatement.append(Formatter.est());
        if(estimationSteps!=null){
            for(EstimationStep estStep : estimationSteps){

                for(EstimationOperation operationType :estStep.getOperations()){
                    String optType = operationType.getOpType();
                    covFound = checkForCovariateStatement(operationType);

                    if(EstimationOpType.EST_POP.value().equals(optType)){
                        estStatement.append(computeMethod(operationType.getAlgorithm()));
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
        String covStatement = (isCovFound()) ? Formatter.cov(): "";
        return Formatter.endline()+covStatement;
    }

    /**
     * Checks if covariate statement exists for estimation operation type and return boolean result.
     * 
     * @param operationType
     * @return
     */
    private Boolean checkForCovariateStatement(EstimationOperation operationType){
        //If covariate is found in any other operations or properties already then return
        if(covFound==true){
            return covFound;
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
            }
        }
        return false;
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

    public List<EstimationStep> getEstimationSteps() {
        return estimationSteps;
    }

    public void setEstimationSteps(List<EstimationStep> estimationSteps) {
        this.estimationSteps = estimationSteps;
    }

    public Boolean isCovFound() {
        return covFound;
    }
}
