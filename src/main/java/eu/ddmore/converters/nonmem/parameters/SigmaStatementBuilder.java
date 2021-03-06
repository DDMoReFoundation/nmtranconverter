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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import crx.converter.engine.FixedParameter;
import crx.converter.spi.blocks.ObservationBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;

/**
 * This class helps to build sigma statement for nmtran file.
 * 
 */
public class SigmaStatementBuilder {

    private final List<String> verifiedSigmas = new ArrayList<String>();
    private List<String> sigmaStatements;
    private StringBuilder sigmaStatementBlock;

    private final Set<ParameterRandomVariable> epsilonVars;
    private final ConversionContext context;


    public SigmaStatementBuilder(ConversionContext context, Set<ParameterRandomVariable> epsilonVars){
        this.context = context;
        this.epsilonVars = epsilonVars;

        initialiseSigmaStatement();
    }

    /**
     * sigma statement block will prepare sigma statement block (and default omega block if omega block is absent but sigma is present) 
     * with help of parameter helper.
     * @return sigma statement
     */
    private void initialiseSigmaStatement() {
        sigmaStatementBlock = new StringBuilder();
        sigmaStatements = getSigmaStatements();

        if(!sigmaStatements.isEmpty()){
            sigmaStatementBlock.append(Formatter.endline()+Formatter.sigma());
            for (final String sigmaVar: sigmaStatements) {
                sigmaStatementBlock.append(sigmaVar);
            }
        }
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

            sigmaRepresentation = RandomVariableHelper.getDistributionTypeStdDev(rv);
            if(sigmaRepresentation==null){
                sigmaRepresentation = RandomVariableHelper.getDistributionTypeVariance(rv);
            }

            isStdDev = RandomVariableHelper.isParamFromStdDev(rv);

            StringBuilder sigmaStatements = new StringBuilder();
            if(isNumeric(sigmaRepresentation)){
                sigmaStatements.append(Double.parseDouble(sigmaRepresentation) +" "+NmConstant.FIX);
                addAttributeForStdDev(sigmaStatements, isStdDev);
                sigmaStatements.append(Formatter.endline());
            }else {
                String sigmastatement = getSigmaFromInitialEstimate(sigmaRepresentation, isStdDev);
                sigmaStatements.append(sigmastatement);
            }
            sigmaParams.add(sigmaStatements.toString());
        }

        return sigmaParams;
    }

    /**
     * Gets random variables from observation as well as parameter block (if associated with residual error)
     * 
     * @return set of random variables
     */
    private Set<ParameterRandomVariable> getRandomVariablesForSigma() {
        Set<ParameterRandomVariable> randomVariableTypes = new LinkedHashSet<ParameterRandomVariable>();

        for(ObservationBlock observationBlock: context.getScriptDefinition().getObservationBlocks()){
            randomVariableTypes.addAll(observationBlock.getRandomVariables());
        }
        randomVariableTypes.addAll(epsilonVars);
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

        for(ParameterEstimate paramEstimate : getAllEstimationParams()){
            String symbId = paramEstimate.getSymbRef().getSymbIdRef();
            if(symbId.equals(varId)){
                Double value = ScalarValueHandler.getValueFromScalarRhs(paramEstimate.getInitialEstimate());
                if(paramEstimate.getInitialEstimate().isFixed()){
                    sigmastatement.append(value+" " + NmConstant.FIX);
                }else{
                    sigmastatement.append(value);
                }
                addAttributeForStdDev(sigmastatement,isStdDev);
                sigmastatement.append(Formatter.indent(Symbol.COMMENT+ symbId)+Formatter.endline());
                addToSigmaVerificationListIfNotExists(symbId);
            }
        }
        return sigmastatement.toString();
    }

    /**
     * Adds identified sigma variables to verified sigmas list. 
     * @param sigmaVar
     */
    private void addToSigmaVerificationListIfNotExists(String sigmaVar){
        if(!verifiedSigmas.contains(sigmaVar))
            verifiedSigmas.add(sigmaVar);
    }

    /**
     * We will need all the estimation parameters to identify sigma params  and at other place, 
     * irrespective of whether they are fixed or not.
     * 
     * @return
     */
    private List<ParameterEstimate> getAllEstimationParams(){
        List<ParameterEstimate> allParams = new ArrayList<ParameterEstimate>();
        allParams.addAll(context.getParameterInitialiser().getParametersToEstimate());
        for(FixedParameter fixedParam : context.getParameterInitialiser().getFixedParameters()){
            allParams.add(fixedParam.pe);
        }
        return allParams;
    }

    /**
     * Adds attribute for standard deviation if statement is for standard deviation.
     * 
     * @param statement
     * @param isStdDev
     */
    private void addAttributeForStdDev(StringBuilder statement, Boolean isStdDev) {
        if(isStdDev){
            statement.append(" "+NmConstant.SD);
        }
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

    public StringBuilder getSigmaStatementBlock() {
        return sigmaStatementBlock;
    }

    public List<String> getVerifiedSigmas() {
        return verifiedSigmas;
    }

}