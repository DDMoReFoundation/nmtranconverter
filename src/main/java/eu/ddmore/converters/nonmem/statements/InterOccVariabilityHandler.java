/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.csvreader.CsvReader;
import com.google.common.base.Preconditions;

import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.commontypes.LevelReference;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;


public class InterOccVariabilityHandler {
    public static final String OCCASION_COL_TYPE = "occasion";
    private ConversionContext context;
    private final List<InputHeader> columns;
    private final Map<String, InputHeader> columnsWithOcc = new HashMap<String, InputHeader>();
    private final Map<Integer, OccRandomVariable> randomVarsWithOcc = new TreeMap<Integer, OccRandomVariable>();
    private final Map<String, List<Double>> iovColumnUniqueValues = new HashMap<String, List<Double>>();

    public InterOccVariabilityHandler(ConversionContext context) throws IOException {
        Preconditions.checkNotNull(context, "Conversion Context cannot be null");
        Preconditions.checkNotNull(context.getInputColumnsHandler().getInputColumns().getInputHeaders(), "columns list cannot be null");

        this.context = context;
        columns = context.getInputColumnsHandler().getInputColumns().getInputHeaders();
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
        }
    }

    private void getOccColumns(){
        for(InputHeader column : columns){
            if(column.getColumnType().name().equalsIgnoreCase(OCCASION_COL_TYPE)){
                columnsWithOcc.put(column.getColumnId(),column);
                iovColumnUniqueValues.put(column.getColumnId(), new ArrayList<Double>());
            }
        }
    }

    private Integer addOccVariabilityRef(ParameterRandomVariable variable, Integer count) {
        if(variable.getVariabilityReference()!=null){
            LevelReference levelRef = variable.getVariabilityReference();
            if(levelRef.getSymbRef().getSymbIdRef()!=null){
                String variabilityRef = levelRef.getSymbRef().getSymbIdRef();

                if(columnsWithOcc.keySet().contains(variabilityRef)){
                    InputHeader column = columnsWithOcc.get(variabilityRef);  
                    OccRandomVariable occRandomVariable = new OccRandomVariable(count, column.getColumnType(), variable);
                    randomVarsWithOcc.put(count,occRandomVariable);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     *  
     * @param iovColumnUniqueValues
     * @throws IOException
     */
    public void retrieveIovColumnUniqueValues(File dataFile) throws IOException{
        if(!iovColumnUniqueValues.isEmpty()){
            CsvReader reader = new CsvReader(dataFile.getAbsolutePath());
            reader.readHeaders();
            while (reader.readRecord())
            {
                for(String columnName : iovColumnUniqueValues.keySet()){
                    List<Double> uniqueValues = iovColumnUniqueValues.get(columnName);
                    Double value = Double.parseDouble(reader.get(columnName));
                    if(!uniqueValues.contains(value)){
                        uniqueValues.add(value);
                    }
                }
            }
            reader.close();
        }
    }

    public Map<Integer, OccRandomVariable> getRandomVarsWithOccasion() {
        return randomVarsWithOcc;
    }

    public Map<String, InputHeader> getColumnsWithOcc() {
        return columnsWithOcc;
    }

    public Map<String, List<Double>> getIovColumnUniqueValues() {
        return iovColumnUniqueValues;
    }

    public class OccRandomVariable{
        private final Integer order;
        private final ParameterRandomVariable variable;
        private final ColumnType relatedColumn;

        public OccRandomVariable(Integer order, ColumnType columnType, ParameterRandomVariable variable) {
            this.order = order;
            this.variable = variable;
            this.relatedColumn = columnType;
        }

        public Integer getOrder() {
            return order;
        }

        public ParameterRandomVariable getVariable() {
            return variable;
        }

        public ColumnType getRelatedColumn() {
            return relatedColumn;
        }
    }
}
