/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.csvreader.CsvReader;
import com.google.common.base.Preconditions;

import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.commontypes.LevelReference;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class handled Inter Occasion Variability in nmtran 
 */
public class InterOccVariabilityHandler {
    private final ConversionContext context;
    private final List<InputColumn> columns;
    private InputColumn columnWithOcc;
    private final Map<Integer, OccRandomVariable> orderedOccRandomVars = new TreeMap<Integer, OccRandomVariable>();
    private final List<String> occasionRandomVariables = new ArrayList<String>();
    private final List<Double> iovColumnUniqueValues = new ArrayList<Double>();

    public InterOccVariabilityHandler(ConversionContext context) throws IOException {
        Preconditions.checkNotNull(context, "Conversion Context cannot be null");
        Preconditions.checkNotNull(context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders(), "columns list cannot be null");

        this.context = context;
        columns = context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders();
        initialise();
    }

    private void initialise() throws IOException{
        Preconditions.checkNotNull(context.getScriptDefinition(), "script definition cannot be null");
        Preconditions.checkNotNull(context.getScriptDefinition().getParameterBlocks(), "parameter blocks cannot be null");

        getOccColumns();
        int count = 1;
        for(ParameterBlock block : context.getScriptDefinition().getParameterBlocks()){
            for(ParameterRandomVariable variable : block.getRandomVariables()){
                count = addOccVariabilityRef(variable, count);
            }
            for(ParameterRandomVariable variable : block.getLinkedRandomVariables()){
                count = addOccVariabilityRef(variable, count);
            }
        }
    }

    private void getOccColumns(){
        for(InputColumn column : columns){
            if(column.getColumnType().name().equalsIgnoreCase(ColumnType.OCCASION.name())){
                columnWithOcc = column;
            }
        }
    }

    private Integer addOccVariabilityRef(ParameterRandomVariable variable, Integer count) {
        if(isRandomVarIOV(variable)){
            Eta eta = new Eta(variable.getSymbId());
            String omegaName = RandomVariableHelper.getNameFromParamRandomVariable(variable);
            eta.setOmegaName(omegaName);
            eta.setOrderInCorr(count);
            OccRandomVariable occRandomVariable = new OccRandomVariable(columnWithOcc.getColumnType(), variable, eta);
            orderedOccRandomVars.put(count,occRandomVariable);
            count++;
            occasionRandomVariables.add(variable.getSymbId());
        }
        return count;
    }

    /**
     * Checks if random variable is with occasional variability level .
     * @param variable
     * @return
     */
    public boolean isRandomVarIOV(ParameterRandomVariable variable){
        boolean isIOV = false;
        if(variable.getVariabilityReference()!=null){
            LevelReference levelRef = variable.getVariabilityReference();
            if(levelRef.getSymbRef().getSymbIdRef()!=null){
                String variabilityRef = levelRef.getSymbRef().getSymbIdRef();

                if(columnWithOcc!=null && columnWithOcc.getColumnId().equals(variabilityRef)){
                    isIOV = true;
                }
            }
        }
        return isIOV;
    }

    /**
     *  gets unique values from iov related columns.
     *  
     * @param iovColumnUniqueValues
     * @throws IOException
     */
    public void retrieveIovColumnUniqueValues(File dataFile) throws IOException{
        if(iovColumnUniqueValues.isEmpty() && columnWithOcc!=null){
            CsvReader reader = new CsvReader(dataFile.getAbsolutePath());
            try{
                reader.readHeaders();
                while (reader.readRecord())
                {
                    Double value = Double.parseDouble(reader.get(columnWithOcc.getColumnId()));
                    if(!iovColumnUniqueValues.contains(value)){
                        iovColumnUniqueValues.add(value);
                    }
                }
            }finally{
                reader.close();
            }
        }
    }

    public Map<Integer, OccRandomVariable> getRandomVarsWithOccasion() {
        return orderedOccRandomVars;
    }

    public InputColumn getColumnWithOcc() {
        return columnWithOcc;
    }

    public List<Double> getIovColumnUniqueValues() {
        return iovColumnUniqueValues;
    }

    public List<String> getOccasionRandomVariables() {
        return occasionRandomVariables;
    }

    public class OccRandomVariable{
        private final ParameterRandomVariable variable;
        private final Eta eta;
        private final ColumnType relatedColumn;

        public OccRandomVariable(ColumnType columnType, ParameterRandomVariable variable, Eta eta) {
            this.variable = variable;
            this.relatedColumn = columnType;
            this.eta = eta;
        }

        public ParameterRandomVariable getVariable() {
            return variable;
        }

        public ColumnType getRelatedColumn() {
            return relatedColumn;
        }

        public Eta getEta() {
            return eta;
        }
    }
}
