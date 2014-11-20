package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.parts.ObservationBlock;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.libpharmml.dom.commontypes.RealValueType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimateType;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class SigmaStatement {
	
	private ParametersHelper parameters;
	List<String> sigmaParams = new ArrayList<String>();

	public SigmaStatement(ParametersHelper parametersHelper){
		parameters = parametersHelper;
	}
	
	/**
	 * This method will get sigma statement as per following algorithm.
	 * 
	 * Get random variables from observation model blocks.
	 * look for symbIdref = 'residual'
	 * if it exists, it will (should) have 'distribution' defined and it has 'stddev' or 'variance'
	 * 
	 * 1.if stddev - 
	 * a. if stddev <var varId="1">
	 * 		$SIGMA
	 * 			1 FIX
	 * b. if stddev <var varId="sigma"> (sigma is example variable it can be anything)
	 * 		there will be given intial estimate for this variable
	 * 		check if attribute is fixed
	 * 		if attribute is 'fixed=true' then "1 FIX"
	 * 		else "1 ;sigma"
	 * c. if stddev <prVal>2</prVal>
	 * 		We need to square this value as "4 FIX"
	 * 
	 * 4. if variance -
	 * 		same as above without squaring the value.
	 *  
	 * @param List<String> list of sigma statements
	 */

	public List<String> getSigmaStatement() {
		
		String sigmaRepresentation = new String();
		
		Set<ParameterRandomVariableType> randomVariableTypes = new HashSet<ParameterRandomVariableType>();
		
		for(ObservationBlock observationBlock: parameters.getScriptDefinition().getObservationBlocks()){
			randomVariableTypes.addAll(observationBlock.getRandomVariables());
		}
		
		for (ParameterRandomVariableType rv : randomVariableTypes) {


			PositiveRealValueType stddevDistribution = parameters.getDistributionTypeStdDev(rv);
			if(stddevDistribution!=null){
				sigmaRepresentation = getSigmaFromStddevDistribution(stddevDistribution);
			}

			PositiveRealValueType varianceDistribution = parameters.getDistributionTypeVariance(rv);
			if(varianceDistribution!=null){
				sigmaRepresentation = getSigmaFromVarianceDistribution(varianceDistribution);
			}

			StringBuilder sigmaStatements = new StringBuilder();
			if(isNumeric(sigmaRepresentation)){
				sigmaStatements.append(Double.parseDouble(sigmaRepresentation) +" FIX\n");
			}else {
				String sigmastatement = getSigmaFromInitialEstimate(sigmaRepresentation);
				sigmaStatements.append(sigmastatement);
			}
			
			sigmaParams.add(sigmaStatements.toString());
			
		}
		return sigmaParams;
	}

	/**
	 * If sigma varId is not a numeric value, it will be variable from intial estimate parameters list.
	 * We need to look for value of this variable and return value of the same. 
	 * 
	 * @param varId
	 * @return
	 */
	private String getSigmaFromInitialEstimate(String varId) {
		String sigmastatement = new String();
		for(ParameterEstimateType params : parameters.getParametersToEstimate()){
			String symbId = params.getSymbRef().getSymbIdRef();
			if(symbId.equals(varId)){
				if(params.getInitialEstimate().isFixed()){
					symbId = "FIX";
				}
				RealValueType value = (RealValueType) params.getInitialEstimate().getScalar().getValue();
				
				sigmastatement = value.getValue()+ " ;"+ symbId+"\n";
				
			}
		}
		return sigmastatement;
	}

	/**
	 * gets sigma value in case of stddev distribution.
	 * 
	 * @param stddevDistribution
	 * @return
	 */
	private String getSigmaFromStddevDistribution(PositiveRealValueType stddevDistribution) {
		String sigmaRepresentation = new String();
		
		if(stddevDistribution!=null){
			if (stddevDistribution.getVar()!=null) {
	        	sigmaRepresentation = stddevDistribution.getVar().getVarId();
	        } else if(stddevDistribution.getPrVal()!=null){
	            Double idVal = (stddevDistribution.getPrVal()*stddevDistribution.getPrVal());
	            sigmaRepresentation = idVal.toString();	            
	        }
		}
		return sigmaRepresentation;
	}

	/**
	 * gets sigma value in case of variance distribution.
	 * 
	 * @param varianceDistribution
	 * @return
	 */
	private String getSigmaFromVarianceDistribution(PositiveRealValueType varianceDistribution) {
		String sigmaRepresentation = new String();
		if(varianceDistribution!=null){
			if (varianceDistribution.getVar()!=null) {
	        	sigmaRepresentation = varianceDistribution.getVar().getVarId();
	        } else if(varianceDistribution.getPrVal()!=null){
	            Double idVal = (varianceDistribution.getPrVal());
	            sigmaRepresentation = idVal.toString();	            
	        }
		}
		return sigmaRepresentation;
	}

	/**
	 * Checks if sigma varId is numeric value or not. if it is then it will be displayed appropriately, 
	 * if its not then it is a variable which needs to be looked into params to estimate list and value can be rendered from there.
	 * 
	 * @param sigmaRepresentation
	 * @return
	 */
	private boolean isNumeric(String sigmaRepresentation) {
        try {
            Double.parseDouble(sigmaRepresentation);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}