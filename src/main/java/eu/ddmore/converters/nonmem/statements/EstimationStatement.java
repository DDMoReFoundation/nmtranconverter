package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import crx.converter.engine.parts.Part;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.modellingsteps.AlgorithmType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOpTypeType;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperationType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class EstimationStatement {
	
	List<String> inputHeaders = new ArrayList<String>();
	String dataFileName = new String();
	List<EstimationStep> estimationSteps = new ArrayList<EstimationStep>();
	Boolean estFIMFound = false;
	
	public List<EstimationStep> getEstimationSteps() {
		return estimationSteps;
	}

	public void setEstimationSteps(List<EstimationStep> estimationSteps) {
		this.estimationSteps = estimationSteps;
	}

	public Boolean isEstFIMFound() {
		return estFIMFound;
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
        	String definition =algorithm.getDefinition(); 
            if (definition.equals("FOCEI")) {
            	sb.append("COND INTER MAXEVALS=9999 PRINT=10 NOABORT");
            }else if (definition.equals("SAEM")) {
            	sb.append("SAEM INTER CTYPE=3 NITER=1000 NBURN=4000 NOPRIOR=1 CITER=10"+Formatter.endline()
            				+"  CALPHA=0.05 IACCEPT=0.4 ISCALE_MIN=1.0E-06 ISCALE_MAX=1.0E+06"+Formatter.endline()
            				+"  ISAMPLE_M1=2 ISAMPLE_M1A=0 ISAMPLE_M2=2 ISAMPLE_M3=2"+Formatter.endline()
            				+"  CONSTRAIN=1 EONLY=0 ISAMPLE=2 PRINT=50");
            } else {
            	sb.append(definition);
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
					if(EstimationOpTypeType.EST_FIM.equals(optType)){
						estFIMFound = true;
					}else if(EstimationOpTypeType.EST_POP.equals(optType)){
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
		String covStatement = (isEstFIMFound()) ? Formatter.endline("$COV"): "";
		return Formatter.endline()+covStatement;
	}
}
