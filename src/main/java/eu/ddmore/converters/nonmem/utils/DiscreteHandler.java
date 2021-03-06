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
package eu.ddmore.converters.nonmem.utils;

import java.util.List;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.blocks.ObservationBlock;
import eu.ddmore.libpharmml.dom.modeldefn.CommonObservationModel;
import eu.ddmore.libpharmml.dom.modeldefn.CountData;
import eu.ddmore.libpharmml.dom.modeldefn.CountPMF;
import eu.ddmore.libpharmml.dom.modeldefn.TTEFunction;
import eu.ddmore.libpharmml.dom.modeldefn.TimeToEventData;
import eu.ddmore.libpharmml.dom.modeldefn.UncertML;
import eu.ddmore.libpharmml.dom.probonto.DistributionName;
import eu.ddmore.libpharmml.dom.probonto.DistributionParameter;
import eu.ddmore.libpharmml.dom.probonto.ParameterName;
import eu.ddmore.libpharmml.dom.probonto.ProbOnto;
import eu.ddmore.libpharmml.dom.uncertml.AbstractDiscreteUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.NaturalNumberValueType;
import eu.ddmore.libpharmml.dom.uncertml.NegativeBinomialDistribution;
import eu.ddmore.libpharmml.dom.uncertml.NegativeBinomialDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;
import eu.ddmore.libpharmml.dom.uncertml.ProbabilityValueType;

/**
 * Handles discrete statement details from pharmML to add to nmtran
 */
public class DiscreteHandler {

    private boolean isDiscrete = false;
    private boolean isCountData = false;
    private boolean isPoissonDist = false;
    private boolean isNegativeBinomial = false;
    private boolean isCategoricalData = false;
    private boolean isTimeToEventData = false;

    private String hazardFunction = new String();
    private StringBuilder discreteStatement;

    public DiscreteHandler(ScriptDefinition definition){
        initialise(definition);
    }

    private void initialise(ScriptDefinition definition) {
        Preconditions.checkNotNull(definition, "Script definition cannot be null");
        setDiscreteStatement(getDiscreteDetails(definition));
    }

    private StringBuilder getDiscreteDetails(ScriptDefinition definition) {
        Preconditions.checkNotNull(definition.getObservationBlocks(), "Observation block cannot be null");

        List<ObservationBlock> blocks = definition.getObservationBlocks();
        StringBuilder discreteStatement = new StringBuilder();
        for(ObservationBlock block :blocks){

            if(block.isDiscrete()){
                setDiscrete(true);
                if(block.getCountData()!=null){
                    setCountData(true);
                    discreteStatement.append(getDiscreteStatements(block.getCountData()));
                } else if(block.getCategoricalData()!=null){
                    setCategoricalData(true);
                    //TODO : Categorical data
                } else if(block.getTimeToEventData()!=null){
                    setTimeToEventData(true);
                    //TODO : Incomplete TTE data
                    discreteStatement.append(getDiscreteStatements(block.getTimeToEventData()));
                }
            }
        }
        return discreteStatement;
    }

    private StringBuilder getDiscreteStatements(CommonObservationModel obsModel){
        StringBuilder statement = new StringBuilder();
        if(obsModel instanceof CountData){
            CountData countData = (CountData) obsModel;
            for(CountPMF countPMF :countData.getListOfPMF()){
                statement.append(getCountDataStatement(countPMF));
            }
        }else if(obsModel instanceof TimeToEventData){
            TimeToEventData tteData = (TimeToEventData) obsModel;
            for(TTEFunction tteFunction : tteData.getListOfHazardFunction()){
                setHazardFunction(tteFunction.getSymbId());
                statement.append(createTimeToEventDataStatements(tteFunction.getSymbId()));
            }
        }
        return statement;
    }

    private StringBuilder getCountDataStatement(CountPMF countPMF) {
        StringBuilder countDistStatement = new StringBuilder();

        if(countPMF.getDistribution().getUncertML()!=null) {
            processUncertMLDistribution(countPMF, countDistStatement);
        } else {
            processProbOntoDistribution(countPMF, countDistStatement);
        }

        return countDistStatement;
    }

    private void processProbOntoDistribution(CountPMF countPMF, StringBuilder countDistStatement) {
        ProbOnto probOnto = countPMF.getDistribution().getProbOnto();
        if(DistributionName.POISSON_1 == probOnto.getName()) {
            String poissonDistVar = getValueOrReference(probOnto.getParameter(ParameterName.RATE));
            countDistStatement.append(createPoissonStatements(poissonDistVar));
        } else if(DistributionName.NEGATIVE_BINOMIAL_1 == probOnto.getName()) {
            String numberOfFailures = getValueOrReference(probOnto.getParameter(ParameterName.NUMBER_OF_FAILURES));
            String probability = getValueOrReference(probOnto.getParameter(ParameterName.PROBABILITY));
            if(probability==null) {
                probability = getValueOrReference(probOnto.getParameter(ParameterName.PROBABILITY_OF_SUCCESS));
            }
            countDistStatement.append(createNegativeBinomialStatement(numberOfFailures, probability));
        }
    }

    private String getValueOrReference(DistributionParameter distributionParameter) {
        if(distributionParameter==null) {
            return null;
        }
        if(distributionParameter.getAssign().getScalar()!=null) {
            return distributionParameter.getAssign().getScalar().valueToString();
        } else {
            return distributionParameter.getAssign().getSymbRef().getSymbIdRef();
        }
        
    }
    private void processUncertMLDistribution(CountPMF countPMF, StringBuilder countDistStatement) {
        UncertML uncertML = countPMF.getDistribution().getUncertML();
        AbstractDiscreteUnivariateDistributionType dist = uncertML.getAbstractDiscreteUnivariateDistribution().getValue();

        if(dist instanceof PoissonDistributionType){
            setPoissonDist(true);
            PoissonDistribution poissonDist = (PoissonDistribution) dist;
            String poissonDistVar = getCountDataValue(poissonDist.getRate());
            countDistStatement.append(createPoissonStatements(poissonDistVar));

        } else if(dist instanceof NegativeBinomialDistributionType){
            setNegativeBinomial(true);
            NegativeBinomialDistribution negativeBinomialDist = (NegativeBinomialDistribution) dist;
            String numberOfFailures = getCountDataValue(negativeBinomialDist.getNumberOfFailures());
            String probability = getCountDataValue(negativeBinomialDist.getProbability());
            countDistStatement.append(createNegativeBinomialStatement(numberOfFailures, probability));
        }
    }

    private String getCountDataValue(PositiveRealValueType valueType){
        Preconditions.checkNotNull(valueType, "Rate value cannot be null for poisson data.");
        if(valueType.getVar()!=null){
            return valueType.getVar().getVarId();
        }else if(valueType.getPrVal()!=null){
            return valueType.getPrVal().toString();
        }else {
            throw new IllegalArgumentException("The specified type doesn't have any value specified.");
        }
    }

    private String getCountDataValue(NaturalNumberValueType valueType){
        Preconditions.checkNotNull(valueType, "Number of failures Value cannot be null for negative binomial");
        if(valueType.getVar()!=null){
            return valueType.getVar().getVarId();
        }else if(valueType.getNVal()!=null){
            return valueType.getNVal().toString();
        }else {
            throw new IllegalArgumentException("The specified type doesn't have any value specified.");
        }
    }

    private String getCountDataValue(ProbabilityValueType valueType){
        Preconditions.checkNotNull(valueType, "Probability Value cannot be null for negative binomial");
        if(valueType.getVar()!=null){
            return valueType.getVar().getVarId();
        }else if(valueType.getPVal()!=null){
            return valueType.getPVal().toString();
        }else {
            throw new IllegalArgumentException("The specified type doesn't have any value specified.");
        }
    }

    private StringBuilder createTimeToEventDataStatements(String tteVar) {
        StringBuilder stringToAdd = new StringBuilder();
        //Adds parts of Error statement at this moment

        stringToAdd.append(Formatter.endline());
        appendLine(stringToAdd,"IF (DV.EQ.0) THEN");
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" Y=EXP(-CUMHAZ) ; likelihood of censored event")));
        appendLine(stringToAdd,"ENDIF");
        appendLine(stringToAdd,"IF (DV.EQ.1) THEN");
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" Y=HAZARD_FUNC*EXP(-CUMHAZ)   ; likelihood of event at exact time")));
        appendLine(stringToAdd,"ENDIF");

        return stringToAdd;
    }

    private StringBuilder createPoissonStatements(String poissonDistVar) {
        StringBuilder stringToAdd = new StringBuilder();

        stringToAdd.append(Formatter.endline());
        appendLine(stringToAdd,"IF (ICALL.EQ.4) THEN");
        appendLine(stringToAdd,Formatter.indent("T=0"));
        appendLine(stringToAdd,Formatter.indent(" N=0"));
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" DO WHILE (T.LT.1)              ;Loop")));
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" CALL RANDOM (2,R)              ;Random number in a uniform distribution")));

        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" T=T-LOG(1-R)/"+poissonDistVar)));
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" IF (T.LT.1) N=N+1")));
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" END DO")));
        appendLine(stringToAdd,Formatter.indent(" DV=N                            ;Incrementation of one integer to the DV"));
        appendLine(stringToAdd,"ENDIF");
        appendLine(stringToAdd,"IF (DV.GT.0) THEN");
        appendLine(stringToAdd,Formatter.indent(" LFAC = GAMLN(DV+1)"));
        appendLine(stringToAdd,"ELSE");
        appendLine(stringToAdd,Formatter.indent(" LFAC=0"));
        appendLine(stringToAdd,"ENDIF");
        stringToAdd.append(Formatter.endline());
        appendLine(stringToAdd,";Logarithm of the Poisson distribution");
        appendLine(stringToAdd,"LPOI = -"+poissonDistVar+"+DV*LOG("+poissonDistVar+")-LFAC");
        appendLine(stringToAdd,";-2 Log Likelihood");
        appendLine(stringToAdd,"Y=-2*(LPOI)");
        return stringToAdd;
    }

    private StringBuilder createNegativeBinomialStatement(String numberOfFailures, String probability ){

        StringBuilder stringToAdd = new StringBuilder();

        stringToAdd.append(Formatter.endline());
        appendLine(stringToAdd,"AGM1=DV + "+numberOfFailures);
        appendLine(stringToAdd,"AGM2="+numberOfFailures);
        appendLine(stringToAdd,"PREC1=(1+1/(12*AGM1))");
        appendLine(stringToAdd,"PREC2=(1+1/(12*AGM2))");
        appendLine(stringToAdd,"GAM1=SQRT(2*3.1415)*(AGM1**(AGM1-0.5))*EXP(-AGM1)*PREC1");
        appendLine(stringToAdd,"GAM2=SQRT(2*3.1415)*(AGM2**(AGM2-0.5))*EXP(-AGM2)*PREC2");
        appendLine(stringToAdd,"FDV=1");
        appendLine(stringToAdd,"IF(DV.GT.0) FDV=SQRT(2*3.1415)*DV**(DV+0.5)*EXP(-DV)*(1+1/(12*DV))");
        appendLine(stringToAdd,"YY=(GAM1/(FDV*GAM2))*( "+probability+"**DV)*(1-"+probability+")**"+numberOfFailures);
        appendLine(stringToAdd,"Y=-2*LOG(YY)");

        return stringToAdd;
    }

    private void appendLine(StringBuilder stringToAdd, String lineToAppend) {
        if(lineToAppend!= null){
            stringToAdd.append(Formatter.endline(lineToAppend));
        }
    }

    public boolean isDiscrete() {
        return isDiscrete;
    }

    public void setDiscrete(boolean isDiscrete) {
        this.isDiscrete = isDiscrete;
    }

    public boolean isCountData() {
        return isCountData;
    }

    public void setCountData(boolean isCountData) {
        this.isCountData = isCountData;
    }

    public boolean isPoissonDist() {
        return isPoissonDist;
    }

    public void setPoissonDist(boolean isPoissonDist) {
        this.isPoissonDist = isPoissonDist;
    }

    public boolean isNegativeBinomial() {
        return isNegativeBinomial;
    }

    public void setNegativeBinomial(boolean isNegativeBinomial) {
        this.isNegativeBinomial = isNegativeBinomial;
    }

    public boolean isCategoricalData() {
        return isCategoricalData;
    }

    public void setCategoricalData(boolean isCategoricalData) {
        this.isCategoricalData = isCategoricalData;
    }

    public boolean isTimeToEventData() {
        return isTimeToEventData;
    }

    public void setTimeToEventData(boolean isTimeToEventData) {
        this.isTimeToEventData = isTimeToEventData;
    }

    public StringBuilder getDiscreteStatement() {
        return discreteStatement;
    }

    public void setDiscreteStatement(StringBuilder discreteStatement) {
        this.discreteStatement = discreteStatement;
    }

    public String getHazardFunction() {
        return hazardFunction;
    }

    public void setHazardFunction(String hazardFunction) {
        this.hazardFunction = hazardFunction;
    }
}