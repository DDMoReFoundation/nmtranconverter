/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.ErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.TableConstant;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

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
        allTables.append(createTableStatement(getStdTableStatement(),TableFile.STD_TABLE.getFileName()));
        allTables.append(createTableStatement(getParamTableStatement(),TableFile.PARAM_TABLE.getFileName()));
        if(!inputColumns.getCatCovTableColumns().isEmpty()){
            allTables.append(createTableStatement(getCatCovTableStatement(),TableFile.CAT_COV_TABLE.getFileName()));
        }
        if(!inputColumns.getContCovTableColumns().isEmpty()){
            allTables.append(createTableStatement(getContCovTableStatement(),TableFile.CONT_COV_TABLE.getFileName()));
        }

        return allTables;
    }

    /**
     * Creates table statement for table type with columns provided. 
     * @param columns
     * @param tableType
     * @return
     */
    private StringBuilder createTableStatement(StringBuilder columns, String tableType){
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
     * @return
     */
    private StringBuilder getStdTableStatement(){

        StringBuilder stdTable = new StringBuilder();
        stdTable.append(SPACE+ColumnConstant.TIME);

        if(!inputColumns.getInputHeaders().isEmpty()){
            for(InputHeader inputHeader : inputColumns.getInputHeaders()){
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

        if(!context.getDiscreteHandler().isDiscrete()){
            stdTable.append(SPACE + TableConstant.PRED + SPACE + ErrorConstant.IPRED + SPACE +
                TableConstant.RES + SPACE + ErrorConstant.IRES + SPACE + TableConstant.WRES + 
                SPACE + ErrorConstant.IWRES );
        }
        stdTable.append(SPACE +ErrorConstant.Y + SPACE + TableConstant.DV);
        return stdTable;
    }

    /**
     * Parameter table contains individual parameters as columns.
     * 
     * @return
     */
    private StringBuilder getParamTableStatement(){
        List<ParameterBlock> blocks =  context.getScriptDefinition().getParameterBlocks();
        StringBuilder paramTable = new StringBuilder();

        InputHeader occColumn = context.getIovHandler().getColumnWithOcc();
        
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
     * 		columnType is "covariate" and valueType is "int"
     * 		with FILE=catab.
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
     * 		columnType is "covariate" and valueType is "real"
     * 		with FILE=cotab.
     * 
     * @return
     */
    public StringBuilder getContCovTableStatement(){
        StringBuilder contCovTable = new StringBuilder();

        for(String column : inputColumns.getContCovTableColumns()){
            contCovTable.append(SPACE+Formatter.getReservedParam(column));	
        }
        return contCovTable;
    }

}
