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
 * This class deals with Occasion random Variabes associated with Inter Occasional variability 
 * and collects information about unique values in occasion column.
 */
public class InterOccVariabilityHandler {
    private final ConversionContext context;
    private InputColumn iovColumn;
    private final Map<Integer, OccRandomVariable> orderedOccRandomVars = new TreeMap<Integer, OccRandomVariable>();
    private final List<String> occasionRandomVariables = new ArrayList<String>();
    private final List<Double> iovColumnUniqueValues = new ArrayList<Double>();

    public InterOccVariabilityHandler(ConversionContext context) throws IOException {
        Preconditions.checkNotNull(context, "Conversion Context cannot be null");
        Preconditions.checkNotNull(context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders(), "columns list cannot be null");

        this.context = context;
        initialise();
    }

    private void initialise() throws IOException{
        Preconditions.checkNotNull(context.getScriptDefinition(), "script definition cannot be null");
        Preconditions.checkNotNull(context.getScriptDefinition().getParameterBlocks(), "parameter blocks cannot be null");

        retrieveOccColumns();
        int count = 1;
        for(ParameterBlock block : context.getScriptDefinition().getParameterBlocks()){
            for(ParameterRandomVariable variable : block.getRandomVariables()){
                count = addOccVariabilityRef(variable, count);
            }
            for(ParameterRandomVariable variable : block.getLinkedRandomVariables()){
                count = addOccVariabilityRef(variable, count);
            }
        }

        File dataFile = context.getDataSetHandler().getDataFile();
        if(dataFile.exists()){
            retrieveIovColumnUniqueValues(dataFile);
        }
    }

    private void retrieveOccColumns(){
        List<InputColumn> columns = context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders();
        for(InputColumn column : columns){
            if(column.getColumnType().name().equalsIgnoreCase(ColumnType.OCCASION.name())){
                iovColumn = column;
                break;
            }
        }
    }

    private Integer addOccVariabilityRef(ParameterRandomVariable variable, Integer count) {
        if(isRandomVarIOV(variable)){
            Eta eta = new Eta(variable.getSymbId());
            String omegaName = RandomVariableHelper.getNameFromParamRandomVariable(variable);
            eta.setOmegaName(omegaName);
            eta.setOrderInCorr(count);
            OccRandomVariable occRandomVariable = new OccRandomVariable(iovColumn.getColumnType(), variable, eta);
            orderedOccRandomVars.put(count,occRandomVariable);
            count++;
            occasionRandomVariables.add(variable.getSymbId());
        }
        return count;
    }

    public boolean isRandomVarIOV(ParameterRandomVariable variable){
        if(variable.getListOfVariabilityReference()!=null){
            for(LevelReference levelRef : variable.getListOfVariabilityReference()){
                if(levelRef.getSymbRef().getSymbIdRef()!=null){
                    String variabilityRef = levelRef.getSymbRef().getSymbIdRef();
                    if(iovColumn!=null && iovColumn.getColumnId().equals(variabilityRef)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * gets unique values from iov related columns where column type is "occasion".
     * @param dataFile nonmem data file
     * @throws IOException
     */
    private void retrieveIovColumnUniqueValues(File dataFile) throws IOException{
        if(iovColumnUniqueValues.isEmpty() && iovColumn!=null){
            CsvReader reader = new CsvReader(dataFile.getAbsolutePath());
            try{
                //Read the first record of data as column headers
                reader.readHeaders();
                while (reader.readRecord())
                {
                    Double value = Double.parseDouble(reader.get(iovColumn.getColumnSequence()-1));
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

    public InputColumn getIovColumn() {
        return iovColumn;
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
