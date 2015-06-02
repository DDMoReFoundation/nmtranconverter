/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.parts.ObservationBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 */
public class SigmaStatementBuilder {

    ParametersHelper paramHelper;

    public SigmaStatementBuilder(ParametersHelper parametersHelper){
        paramHelper = parametersHelper; 
    }

    /**
     * sigma statement block will prepare sigma statement block (and default omega block if omega block is absent but sigma is present) 
     * with help of parameter helper.
     * @return sigma statement
     */
    public StringBuilder getSigmaStatementBlock() {
        StringBuilder sigmaStatement = new StringBuilder();
        List<String> sigmaStatements = getSigmaStatements();

        if(!sigmaStatements.isEmpty()){
            //adding default Omega if omega block is absent but sigma is present 
            if(paramHelper.omegaDoesNotExist()){
                sigmaStatement.append(Formatter.endline());
                sigmaStatement.append(Formatter.endline(Formatter.omega()+"0 "+NmConstant.FIX));
            }
            sigmaStatement.append(Formatter.endline()+Formatter.sigma());
            for (final String sigmaVar: sigmaStatements) {
                sigmaStatement.append(sigmaVar);
            }
        }
        return sigmaStatement;
    }

    /**
     * This method will get sigma statement as per following algorithm.
     * 
     * Get random variables from observation model blocks.
     * 
     * Get residual errors from observation model and look for any random variables defined anywhere in parameter model blocks.
     * If there is any such parameter random variable, then add it to random variables list to add as Sigma.  
     * 
     * if it exists, it will (should) have 'distribution' defined and it has 'stddev' or 'variance'
     * 
     * 1.if stddev - 
     * a. if stddev <var varId="1">
     * 		$SIGMA
     * 			1 FIX
     * b. if stddev <var varId="sigma"> (sigma is example variable it can be anything)
     * 		there will be given initial estimate for this variable
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
     * @return List<String> list of sigma statements
     */

    private List<String> getSigmaStatements() {

        List<String> sigmaParams = new ArrayList<String>();
        String sigmaRepresentation = new String();

        Set<ParameterRandomVariable> randomVariableTypes = getRandomVariablesForSigma();

        for (ParameterRandomVariable rv : randomVariableTypes) {

            Boolean isStdDev = false;

            PositiveRealValueType stddevDistribution = RandomVariableHelper.getDistributionTypeStdDev(rv);
            if(stddevDistribution!=null){
                sigmaRepresentation = getSigmaFromStddevDistribution(stddevDistribution);
            }

            PositiveRealValueType varianceDistribution = RandomVariableHelper.getDistributionTypeVariance(rv);
            if(varianceDistribution!=null){
                sigmaRepresentation = getSigmaFromVarianceDistribution(varianceDistribution);
            }

            isStdDev = RandomVariableHelper.isParamFromStdDev(rv);

            StringBuilder sigmaStatements = new StringBuilder();
            if(isNumeric(sigmaRepresentation)){
                sigmaStatements.append(Double.parseDouble(sigmaRepresentation) +" "+NmConstant.FIX);
            }else {
                String sigmastatement = getSigmaFromInitialEstimate(sigmaRepresentation, isStdDev);
                sigmaStatements.append(sigmastatement);
            }
            addAttributeForStdDev(sigmaStatements,isStdDev);
            sigmaStatements.append(Formatter.endline());
            sigmaParams.add(sigmaStatements.toString());
        }
        return sigmaParams;
    }

    /**
     * Gets random variables from observation as well as parameter block (if associated with residual error)
     * 
     * @param context
     * @return set of random variables
     */
    private Set<ParameterRandomVariable> getRandomVariablesForSigma() {
        Set<ParameterRandomVariable> randomVariableTypes = new HashSet<ParameterRandomVariable>();

        for(ObservationBlock observationBlock: paramHelper.getScriptDefinition().getObservationBlocks()){
            randomVariableTypes.addAll(observationBlock.getRandomVariables());
        }
        randomVariableTypes.addAll(ConversionContext.getEpsilonRandomVariables(paramHelper.getScriptDefinition()));
        return randomVariableTypes;
    }

    /**
     * If sigma varId is not a numeric value, it will be variable from initial estimate parameters list.
     * We need to look for value of this variable and return value of the same. 
     * 
     * @param varId - sigma var id
     * @param isStdDev - std dev flag
     * @return sigma - statement string
     */
    private String getSigmaFromInitialEstimate(String varId, Boolean isStdDev) {
        StringBuilder sigmastatement = new StringBuilder();

        for(ParameterEstimate paramEstimate : paramHelper.getAllEstimationParams()){
            String symbId = paramEstimate.getSymbRef().getSymbIdRef();
            if(symbId.equals(varId)){
                Double value = ScalarValueHandler.getValueFromScalarRhs(paramEstimate.getInitialEstimate());
                if(paramEstimate.getInitialEstimate().isFixed()){
                    sigmastatement.append(value+" " + NmConstant.FIX);
                }else{
                    sigmastatement.append(value);
                }
                addAttributeForStdDev(sigmastatement,isStdDev);
                sigmastatement.append(Formatter.endline(Formatter.indent(Symbol.COMMENT+ symbId)));
                paramHelper.addToSigmaVerificationListIfNotExists(symbId);
            }
        }
        return sigmastatement.toString();
    }

    /**
     * Adds attribute for standard deviation if statement is for standard deviation.
     * 
     * @param statement
     * @param isStdDev
     */
    private void addAttributeForStdDev(StringBuilder statement, Boolean isStdDev) {
        if(isStdDev){
            statement.append(Formatter.endline(" "+NmConstant.SD));
        }
    }

    /**
     * Gets sigma representation for standard deviation distribution.
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
     * Gets sigma representation for standard deviation variance.
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