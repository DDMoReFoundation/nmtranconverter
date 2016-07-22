/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.spi.blocks.ParameterBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.error.ErrorStatement;
import eu.ddmore.converters.nonmem.statements.error.GeneralObsErrorStatement;
import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.statements.input.InputColumn;
import eu.ddmore.converters.nonmem.statements.input.InputColumnsProvider;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.TableConstant;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * This class creates Table statement for nmtran with help of conversion context.
 */
public class TableStatement {

    private static final String SPACE = " ";

    enum TableFile{
        STD_TABLE("sdtab"),
        PARAM_TABLE("patab"),
        CAT_COV_TABLE("catab"),
        CONT_COV_TABLE("cotab");

        private String fileName;

        private TableFile(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return this.fileName;
        }
    }
    private final ConversionContext context;
    private InputColumnsProvider inputColumns;

    public TableStatement(ConversionContext convContext){
        Preconditions.checkNotNull(convContext, "Conversion Context cannot be null");
        this.context = convContext;
        Preconditions.checkNotNull(context.getInputColumnsHandler(), "input columns handler cannot be null.");
        Preconditions.checkNotNull(context.getInputColumnsHandler().getInputColumnsProvider(), "input columns provider cannot be null.");
        inputColumns = context.getInputColumnsHandler().getInputColumnsProvider(); 
    }

    /**
     * Returns all table statements for nmtran file.
     * 
     * @return string builder with table statements 
     */
    public StringBuilder getStatements(){
        StringBuilder allTables = new StringBuilder();
        allTables.append(createTableStatement(getStdTableStatement().toString(),TableFile.STD_TABLE.getFileName()));

        String paramTableStatement = getParamTableStatement().toString();
        if(!paramTableStatement.isEmpty()){
            allTables.append(createTableStatement(paramTableStatement,TableFile.PARAM_TABLE.getFileName()));
        }
        if(!inputColumns.getCatCovTableColumns().isEmpty()){
            allTables.append(createTableStatement(getCatCovTableStatement().toString(),TableFile.CAT_COV_TABLE.getFileName()));
        }
        if(!inputColumns.getContCovTableColumns().isEmpty()){
            allTables.append(createTableStatement(getContCovTableStatement().toString(),TableFile.CONT_COV_TABLE.getFileName()));
        }

        return allTables;
    }

    /**
     * Creates table statement for table type with columns provided. 
     * @param columns
     * @param tableType
     * @return table statement
     */
    private StringBuilder createTableStatement(String columns, String tableType){
        StringBuilder tableStatement = new StringBuilder();
        tableStatement.append(Formatter.endline());
        tableStatement.append(Formatter.table());
        tableStatement.append(SPACE+ColumnConstant.ID);
        tableStatement.append(columns+SPACE+TableConstant.NOAPPEND+SPACE+TableConstant.NOPRINT);
        tableStatement.append(Formatter.endline(" FILE="+tableType));
        return tableStatement;
    }

    /**
     * Standard table contains columns listed as part of input statement 
     * and variables defined in Error model along with 'DV' at the end.
     * 
     * @return standard table statement
     */
    private StringBuilder getStdTableStatement(){
        StringBuilder stdTable = new StringBuilder();
        stdTable.append(SPACE+ColumnConstant.TIME);

        if(!inputColumns.getInputHeaders().isEmpty()){
            for(InputColumn inputHeader : inputColumns.getInputHeaders()){
                String columnId = inputHeader.getColumnId();
                boolean isDV = columnId.equals(Formatter.getReservedParam(TableConstant.DV.toString()));
                // Adding ID TIME at start and DV at the end hence skipping here.
                if(inputHeader.isDropped() || columnId.equals(ColumnConstant.ID.toString()) || 
                        columnId.equals(ColumnConstant.TIME.toString()) || isDV){
                    continue;
                }
                stdTable.append(SPACE+Formatter.getReservedParam(columnId));
            }
        }

        Set<String> columnNames = getColumnNamesForStabFromErrorDetails();

        for(String columnName : columnNames){
            stdTable.append(SPACE+columnName);
        }
        return stdTable;
    }

    private Set<String> getColumnNamesForStabFromErrorDetails() {
        Set<String> columnNames = new LinkedHashSet<>();
        boolean isStructuralObsError = false;
        boolean isGeneralObsError = false;
        Set<String> varsFromGeneralObsError= new HashSet<>();

        for(ErrorStatement error : context.getModelStatementHelper().getErrorStatementHandler().getErrorStatements().values()){
            if(error.isStructuralObsError()){
                isStructuralObsError = true;
            }else{
                isGeneralObsError = true;
                GeneralObsErrorStatement generalObsError = (GeneralObsErrorStatement) error;
                varsFromGeneralObsError.addAll(generalObsError.getVarEquations().keySet());
            }
        }

        if(isGeneralObsError){
            addTableConstants(columnNames);
            if(isStructuralObsError){
                addErrorConstants(columnNames);
            }
            for(String variable: varsFromGeneralObsError){
                columnNames.add(variable);
            }
        }else if(!context.getDiscreteHandler().isDiscrete()){
            addTableConstants(columnNames);
            addErrorConstants(columnNames);
        }
        return columnNames;
    }

    private void addErrorConstants(Set<String> columnNames) {
        columnNames.add(ErrorConstant.IPRED.toString());
        columnNames.add(ErrorConstant.IRES.toString());
        columnNames.add(ErrorConstant.IWRES.toString());
        columnNames.add(ErrorConstant.Y.toString());
    }

    private void addTableConstants(Set<String> columnNames) {
        columnNames.add(TableConstant.PRED.toString());
        columnNames.add(TableConstant.RES.toString());
        columnNames.add(TableConstant.WRES.toString());
        columnNames.add(TableConstant.DV.toString());
    }

    /**
     * Parameter table contains individual parameters as columns.
     * @return parameter table statement
     */
    private StringBuilder getParamTableStatement(){
        List<ParameterBlock> blocks =  context.getScriptDefinition().getParameterBlocks();
        StringBuilder paramTable = new StringBuilder();
        InputColumn occColumn = context.getIovHandler().getIovColumn();
        if(occColumn!=null && StringUtils.isNotEmpty(occColumn.getColumnId())){
            paramTable.append(SPACE+Formatter.getReservedParam(occColumn.getColumnId()));
        }
        for(ParameterBlock block : blocks){
            List<IndividualParameter> indivParamTypes = block.getIndividualParameters();
            for(IndividualParameter parameterType: indivParamTypes){
                paramTable.append(SPACE+Formatter.getReservedParam(parameterType.getSymbId()));
            }
        }
        Set<Eta> orderedEtas = context.retrieveOrderedEtas();
        for(Eta eta : orderedEtas){
            paramTable.append(SPACE+Formatter.getReservedParam(eta.getEtaSymbol()));
        }
        return paramTable;
    }

    /**
     * Categorical cov tables contains columns from nonmem dataset where, 
     * columnType is "covariate" and valueType is "int"
     * with FILE=catab.
     * 
     * @return
     */
    private StringBuilder getCatCovTableStatement(){
        StringBuilder catCovTable = new StringBuilder();
        for(String column : inputColumns.getCatCovTableColumns()){
            catCovTable.append(SPACE+Formatter.getReservedParam(column));
        }
        return catCovTable;
    }

    /**
     * Continuous cov tables contains columns from nonmem dataset where, 
     * columnType is "covariate" and valueType is "real"
     * with FILE=cotab.
     * 
     * @return continuous cov statement
     */
    private StringBuilder getContCovTableStatement(){
        StringBuilder contCovTable = new StringBuilder();
        for(String column : inputColumns.getContCovTableColumns()){
            contCovTable.append(SPACE+Formatter.getReservedParam(column));	
        }
        return contCovTable;
    }
}
