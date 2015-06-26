package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ObservationBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.modeldefn.CommonObservationModel;
import eu.ddmore.libpharmml.dom.modeldefn.CountData;
import eu.ddmore.libpharmml.dom.modeldefn.CountPMF;
import eu.ddmore.libpharmml.dom.modeldefn.TTEFunction;
import eu.ddmore.libpharmml.dom.modeldefn.TimeToEventData;
import eu.ddmore.libpharmml.dom.uncertml.NegativeBinomialDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;

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
    
    private StringBuilder discreteStatement;
    
    public DiscreteHandler(ScriptDefinition definition){
        initialise(definition);
        
    }

    private void initialise(ScriptDefinition definition) {
        setDiscreteStatement(getDiscreteDetails(definition));
    }

    /**
     * Gets details for discrete statements from observation blocks.
     * 
     * @param convContext
     * @return discrete statement with details
     */
    private StringBuilder getDiscreteDetails(ScriptDefinition definition) {
        List<ObservationBlock> blocks = definition.getObservationBlocks();
        StringBuilder discreteStatement = new StringBuilder();
        for(ObservationBlock block :blocks){
            
            if(block.isDiscrete()){
                setDiscrete(true);
                if(block.getCountData()!=null){
                    setCountData(true);
                    List<String> distVars = getDiscreteVariables(block.getCountData());
                    for(String poissonDistVar : distVars){
                        discreteStatement.append(createCountDataStatements(poissonDistVar));
                    }
                } else if(block.getCategoricalData()!=null){
                    setCategoricalData(true);
                    //TODO : Categorical stuff

                } else if(block.getTimeToEventData()!=null){
                    setTimeToEventData(true);
                    List<String> tteVars = getDiscreteVariables(block.getTimeToEventData());
                    for(String timeToEventVar : tteVars){
                        discreteStatement.append(createTimeToEventDataStatements(timeToEventVar));
                    }
                }
            }
        }
        return discreteStatement;
    }
    
    /**
     * Retrieves required variables related to discrete block from common observation model.
     * 
     * @param obsModel
     * @return
     */
    private List<String> getDiscreteVariables(CommonObservationModel obsModel){
        List<String> variables = new ArrayList<String>();
        if(obsModel instanceof CountData){
            CountData countData = (CountData) obsModel;
            for(CountPMF countPMF :countData.getListOfPMF()){
                variables.add(getPoissonDistributionVariable(countPMF));
            }
        }else if(obsModel instanceof TimeToEventData){
            TimeToEventData tteData = (TimeToEventData) obsModel;
            for(TTEFunction tteFunction : tteData.getListOfHazardFunction()){
                variables.add(tteFunction.getSymbId());
            }
        }
        return variables;
    }
    
    /**
     *  Gets distribution variable specified for discrete block.
     *  
     * @param block
     * @return
     */
    private String getPoissonDistributionVariable(CountPMF countPMF) {
        String poissonDistVar = new String();
        if(countPMF.getDistribution() instanceof PoissonDistribution){
            setPoissonDist(true);
            PoissonDistribution poissonDist = (PoissonDistribution) countPMF.getDistribution();
            if(poissonDist.getRate()!=null){
               if(poissonDist.getRate().getVar()!=null){
                   poissonDistVar = poissonDist.getRate().getVar().getVarId();       
               } else if(poissonDist.getRate().getPrVal()!=null){
                   poissonDistVar = poissonDist.getRate().getPrVal().toString();
               }else {
                   throw new IllegalArgumentException("The poisson distribution doesn't have any value specified for rate.");
               }
            }
        } else if (countPMF.getDistribution() instanceof NegativeBinomialDistribution){
            setNegativeBinomial(true);
//            NegativeBinomialDistribution negativeBinomialDist = (NegativeBinomialDistribution) countPMF.getDistribution();   
        }
        return poissonDistVar;
    }
    
    /**
     * Create statements for time to event data with help of the variable provided.
     * 
     * @param tteVar
     * @return
     */
    private StringBuilder createTimeToEventDataStatements(String tteVar) {
        StringBuilder stringToAdd = new StringBuilder();
        stringToAdd.append(Formatter.endline());
        appendLine(stringToAdd,"IF (ICALL.EQ.4) THEN");
        appendLine(stringToAdd,Formatter.indent(Formatter.indent(" ; ADD TTE DETAILS FOR "+tteVar)));
        appendLine(stringToAdd,"ENDIF");
        return stringToAdd;
    }

    /**
     * Create statements for time to event data with help of the poisson distribution variable provided.
     * 
     * @param poissonDistVar
     * @return
     */
    private StringBuilder createCountDataStatements(String poissonDistVar) {
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

    /**
     * Adds line at the end of the string build and appends it with end of line. 
     * @param stringToAdd
     * @param lineToAppend
     */
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
}