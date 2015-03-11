/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.statements.ErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.ColumnConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.TableConstant;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;

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
	private ScriptDefinition scriptDefinition;
	private InputStatement inputStatement = null;
	

	public TableStatement(ScriptDefinition scriptDefinition, InputStatement inputStatement){
		this.scriptDefinition = scriptDefinition;
		if(inputStatement == null){
			throw new IllegalStateException("Input statement cannot be null and needs to be populated.");
		}else{
			this.inputStatement = inputStatement;
		}
		
	}
	
	public StringBuilder getStatements(){
		StringBuilder allTables = new StringBuilder();
		allTables.append(createTableStatement(getStdTableStatement(),TableFile.STD_TABLE.getFileName()));
		allTables.append(createTableStatement(getParamTableStatement(),TableFile.PARAM_TABLE.getFileName()));
		if(!inputStatement.getCatCovTableColumns().isEmpty()){
			allTables.append(createTableStatement(getCatCovTableStatement(),TableFile.CAT_COV_TABLE.getFileName()));
		}
		if(!inputStatement.getContCovTableColumns().isEmpty()){
			allTables.append(createTableStatement(getContCovTableStatement(),TableFile.CONT_COV_TABLE.getFileName()));
		}
		
		return allTables;
	}
	
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
		if(!inputStatement.getInputHeaders().isEmpty()){
			for(String inputHeader : inputStatement.getInputHeaders()){
				// Adding ID TIME at start and DV at the end hence skipping here.
				if(inputHeader.equals(ColumnConstant.ID.toString()) || 
						inputHeader.equals(ColumnConstant.TIME.toString()) || inputHeader.equals(TableConstant.DV.toString())){
					continue;
				}
				stdTable.append(SPACE+inputHeader);
			}
		}
		stdTable.append(SPACE + TableConstant.PRED + SPACE + ErrorConstant.IPRED + SPACE +
				TableConstant.RES + SPACE + ErrorConstant.IRES + SPACE + TableConstant.WRES + 
				SPACE + ErrorConstant.IWRES + SPACE);
		stdTable.append(ErrorConstant.Y + SPACE + TableConstant.DV);
		return stdTable;
	}
	
	/**
	 * Parameter table contains individual parameters as columns.
	 * 
	 * @return
	 */
	private StringBuilder getParamTableStatement(){
		List<ParameterBlock> blocks =  scriptDefinition.getParameterBlocks();
		StringBuilder paramTable = new StringBuilder();

		for(ParameterBlock block : blocks){
			List<IndividualParameterType> indivParamTypes = block.getIndividualParameters();
			for(IndividualParameterType parameterType: indivParamTypes){
				paramTable.append(SPACE+parameterType.getSymbId());
			}
		}
		for(String eta : ParametersHelper.getAllEtas(scriptDefinition)){
			paramTable.append(SPACE+eta.toUpperCase());
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
		
		for(String column : inputStatement.getCatCovTableColumns()){
			catCovTable.append(SPACE+column);	
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
		
		for(String column : inputStatement.getContCovTableColumns()){
			contCovTable.append(SPACE+column);	
		}
		return contCovTable;
	}
	
}
