/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.BooleanType;
import eu.ddmore.libpharmml.dom.commontypes.TrueBooleanType;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.modellingsteps.AlgorithmType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpTypeType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperationType;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationPropertyType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class EstimationStatement {
	enum Method{
		FO, FOCE, FOCEI, SAEM
	}
	
	List<String> inputHeaders = new ArrayList<String>();
	String dataFileName = new String();
	List<EstimationStep> estimationSteps = new ArrayList<EstimationStep>();
	static Boolean covFound = false;
	
	public List<EstimationStep> getEstimationSteps() {
		return estimationSteps;
	}

	public void setEstimationSteps(List<EstimationStep> estimationSteps) {
		this.estimationSteps = estimationSteps;
	}

	public Boolean isCovFound() {
		return covFound;
	}

	public EstimationStatement(ScriptDefinition scriptDefinition){
		estimationSteps = filterOutEstimationSteps(scriptDefinition);
	}

	/**
	 * Compute method for algorithm depending upon its definition.
	 * 
	 */
	private StringBuilder computeMethod(AlgorithmType algorithm) {
        StringBuilder sb = new StringBuilder();
        sb.append("METHOD=");
        if (algorithm!=null) {
        	String methodDefinition =algorithm.getDefinition();
        	if (methodDefinition.equals(Method.FO.toString())) {
        		sb.append("ZERO MAXEVALS=9999 PRINT=10 NOABORT");
        	}
        	else if(methodDefinition.equals(Method.FOCE.toString())) {
        		sb.append("COND MAXEVALS=9999 PRINT=10 NOABORT");
        	}
        	else if (methodDefinition.equals(Method.FOCEI.toString())) {
            	sb.append("COND INTER MAXEVALS=9999 PRINT=10 NOABORT");
            }
        	else if (methodDefinition.equals(Method.SAEM.toString())) {
            	sb.append("SAEM INTER CTYPE=3 NITER=1000 NBURN=4000 NOPRIOR=1 CITER=10"+Formatter.endline()
            				+"  CALPHA=0.05 IACCEPT=0.4 ISCALE_MIN=1.0E-06 ISCALE_MAX=1.0E+06"+Formatter.endline()
            				+"  ISAMPLE_M1=2 ISAMPLE_M1A=0 ISAMPLE_M2=2 ISAMPLE_M3=2"+Formatter.endline()
            				+"  CONSTRAIN=1 EONLY=0 ISAMPLE=2 PRINT=50");
            }
        	else {
            	sb.append(methodDefinition);
            }
        } else {
            sb.append("COND");
        }
        return sb;
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
		estStatement.append("$EST ");
		if(estimationSteps!=null){
			for(EstimationStep estStep : estimationSteps){
	
				for(EstimationOperationType operationType :estStep.getOperations()){
					EstimationOpTypeType optType = operationType.getOpType();
					covFound = checkForCovariateStatement(operationType);
					
					if(EstimationOpTypeType.EST_POP.equals(optType)){
						estStatement.append(computeMethod(operationType.getAlgorithm()));
					}else if(EstimationOpTypeType.EST_INDIV.equals(optType)){
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
		String covStatement = (isCovFound()) ? Formatter.endline("$COV"): "";
		return Formatter.endline()+covStatement;
	}
	
	/**
	 * Checks if covariate statement exists for estimation operation type and return boolean result.
	 * 
	 * @param operationType
	 * @return
	 */
	private Boolean checkForCovariateStatement(EstimationOperationType operationType){
		//If covariate is found in any other operations or properties already then return
		if(covFound==true){
			return covFound;
		}
		EstimationOpTypeType optType = operationType.getOpType();
		if(EstimationOpTypeType.EST_FIM.equals(optType)){
			return true;
		}else if(EstimationOpTypeType.EST_POP.equals(optType)){
			for(OperationPropertyType property : operationType.getProperty()){
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
		if(value instanceof BooleanType){
			BooleanType val = (BooleanType) value;
			return (val instanceof TrueBooleanType);
		}
		return false;
	}
}
