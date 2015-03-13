/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.parts.ObservationBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.Constant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.commontypes.RealValueType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimateType;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 */
public class SigmaStatementBuilder {

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
     * 		if attribute is 'fixed=true' 
     * 			"1 FIX ;sigma"
     * 		else "1 ;sigma"
     * c. if stddev <prVal>2</prVal>
     * 		We need to square this value as "4 FIX"
     * 
     * 4. if variance -
     * 		same as above without squaring the value.
     *  
     * @param ParametersHelper
     * @return List<String> list of sigma statements
     */

    public List<String> getSigmaStatements(ParametersHelper parameters) {

        List<String> sigmaParams = new ArrayList<String>();

        String sigmaRepresentation = new String();

        Set<ParameterRandomVariableType> randomVariableTypes = new HashSet<ParameterRandomVariableType>();

        for(ObservationBlock observationBlock: parameters.getScriptDefinition().getObservationBlocks()){
            randomVariableTypes.addAll(observationBlock.getRandomVariables());
        }

        for (ParameterRandomVariableType rv : randomVariableTypes) {

            Boolean isStdDev = false;

            PositiveRealValueType stddevDistribution = parameters.getDistributionTypeStdDev(rv);
            if(stddevDistribution!=null){
                sigmaRepresentation = getSigmaFromStddevDistribution(stddevDistribution);
            }

            PositiveRealValueType varianceDistribution = parameters.getDistributionTypeVariance(rv);
            if(varianceDistribution!=null){
                sigmaRepresentation = getSigmaFromVarianceDistribution(varianceDistribution);
            }

            isStdDev = parameters.isParamFromStdDev(rv);

            StringBuilder sigmaStatements = new StringBuilder();
            if(isNumeric(sigmaRepresentation)){
                sigmaStatements.append(Double.parseDouble(sigmaRepresentation) +" "+Constant.FIX);
            }else {
                String sigmastatement = getSigmaFromInitialEstimate(sigmaRepresentation, isStdDev, parameters);
                sigmaStatements.append(sigmastatement);
            }
            addAttributeForStdDev(sigmaStatements,isStdDev);
            sigmaStatements.append(Formatter.endline());
            sigmaParams.add(sigmaStatements.toString());
        }
        return sigmaParams;
    }

    /**
     * If sigma varId is not a numeric value, it will be variable from initial estimate parameters list.
     * We need to look for value of this variable and return value of the same. 
     * 
     * @param varId - sigma var id
     * @param isStdDev - std dev flag
     * @return sigma - statement string
     */
    private String getSigmaFromInitialEstimate(String varId, Boolean isStdDev, ParametersHelper parameters) {
        StringBuilder sigmastatement = new StringBuilder();

        for(ParameterEstimateType params : parameters.getAllEstimationParams()){
            String symbId = params.getSymbRef().getSymbIdRef();
            if(symbId.equals(varId)){
                RealValueType value = (RealValueType) params.getInitialEstimate().getScalar().getValue();
                sigmastatement.append(value.getValue());
                if(params.getInitialEstimate().isFixed()){
                    sigmastatement.append(" " + Constant.FIX);
                }else{
                    sigmastatement.append(value.getValue());
                }
                addAttributeForStdDev(sigmastatement,isStdDev);
                sigmastatement.append(Formatter.endline(Formatter.indent(Symbol.COMMENT+ symbId)));
                parameters.addToSigmaListIfNotExists(symbId);
            }
        }
        return sigmastatement.toString();
    }

    public void addAttributeForStdDev(StringBuilder statement, Boolean isStdDev) {
        if(isStdDev){
            statement.append(Formatter.endline(" "+Constant.SD));
        }
    }

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
     * Checks if sigma varId is numeric value or not. if it is then it will be displayed appropriately. 
     * 
     * @param representation
     * @return
     */
    private boolean isNumeric(String representation) {
        try {
            Double.parseDouble(representation);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}