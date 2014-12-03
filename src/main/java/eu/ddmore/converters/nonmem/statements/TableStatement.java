package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.ParameterBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameterType;

public class TableStatement {
	
	private static final String WRES = "WRES";

	private static final String RES = "RES";

	private static final String PRED = "PRED";

	private static final String NOPRINT = "NOPRINT";

	private static final String DV = "DV";

	ScriptDefinition scriptDefinition;
	
	public static final String TABLE = "\n$TABLE ";
	public static final String ID = "ID";
	public static final String TIME = "TIME";
	public static final String NOAPPEND = "NOAPPEND";
	public static final String STD_TABLE_FILE = "sdtab";
	public static final String PARAM_TABLE_FILE = "patab";
	public static final String CAT_COV_TABLE_FILE = "catab";
	public static final String CONT_COV_TABLE_FILE = "cotab";
	
	InputStatement inputStatement = null;
	

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
		allTables.append(createTableStatement(getStdTableStatement(), STD_TABLE_FILE));
		allTables.append(createTableStatement(getParamTableStatement(), PARAM_TABLE_FILE));
		if(!inputStatement.getCatCovTableColumns().isEmpty()){
			allTables.append(createTableStatement(getCatCovTableStatement(), CAT_COV_TABLE_FILE));
		}
		if(!inputStatement.getContCovTableColumns().isEmpty()){
			allTables.append(createTableStatement(getContCovTableStatement(), CONT_COV_TABLE_FILE));
		}
		
		return allTables;
	}
	
	private StringBuilder createTableStatement(StringBuilder columns, String tableType){
		StringBuilder tableStatement = new StringBuilder();
		tableStatement.append(TABLE);
		
		tableStatement.append(ID+" "+TIME);
		tableStatement.append(columns+" "+NOAPPEND+" "+NOPRINT);
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

		if(!inputStatement.getInputHeaders().isEmpty()){
			for(String inputHeader : inputStatement.getInputHeaders()){
				// Adding ID TIME at start and DV at the end hence skipping here.
				if(inputHeader.equals(ID) || inputHeader.equals(TIME) || inputHeader.equals(DV)){
					continue;
				}
				stdTable.append(" "+inputHeader);
			}
		}
		stdTable.append(" "+PRED+" "+ErrorStatement.IPRED+" "+RES+" "+ErrorStatement.IRES+" "+WRES+" "+ErrorStatement.IWRES+" ");
		stdTable.append(ErrorStatement.Y+" "+DV);
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
				paramTable.append(" "+parameterType.getSymbId());
			}
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
			catCovTable.append(" "+column);	
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
			contCovTable.append(" "+column);	
		}
		return contCovTable;
	}
	
}
