/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.converters.nonmem.statements.ErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;

public enum ErrorType {
	
	COMBINED_ERROR_1("combinedError1"){
		@Override
		StringBuilder getErrorStatement() {
			String weight = ErrorStatement.additive+
					"+"+ErrorStatement.proportional+"*"+ErrorConstant.IPRED;
			return createErrorStatement(weight);
		};
	},
	COMBINED_ERROR_2("combinedError2"){
		@Override
		StringBuilder getErrorStatement() {
			String weight = "SQRT(("+ErrorStatement.additive+"*"+
					ErrorStatement.additive+")"+"+ ("+ErrorStatement.proportional+"*"+
					ErrorStatement.proportional+"*"+ErrorConstant.IPRED+"*"+ErrorConstant.IPRED+"))";
			return createErrorStatement(weight);
		};
	},
	COMBINED_ERROR_3("combinedError3"){
		@Override
		StringBuilder getErrorStatement() {
			return COMBINED_ERROR_2_LOG.getErrorStatement();
		}
	},
	COMBINED_ERROR_2_LOG("combinedError2log"){

		@Override
		StringBuilder getErrorStatement() {
			String iPred = "LOG("+ErrorStatement.functionRep+")";
			String weight = "SQRT("+ErrorStatement.proportional+"**2"+
					" + ("+ErrorStatement.additive+"/"+ErrorStatement.functionRep+")**2)";
			return createErrorStatement(iPred, weight, null, null, null);
		};
		
	},
	ADDITIVE_ERROR("additiveError"){
		@Override
		StringBuilder getErrorStatement() {
			String weight = ErrorStatement.additive;
			return createErrorStatement(weight);
		};
	},
	PROPORTIONAL_ERROR("proportionalError"){
		@Override
		StringBuilder getErrorStatement() {
			String weight = ErrorStatement.proportional+"*"+ErrorConstant.IPRED;
			return createErrorStatement(weight);
		};
	};
	
	
	private String error;
	
	private static String DEFAULT_IPRED = ErrorStatement.functionRep;
	private static final String DEFAULT_Y = ErrorConstant.IPRED+"+"+ErrorConstant.W+"*EPS(1)";
	private static final String DEFAULT_WEIGHT = " "+ErrorConstant.W;
	private static final String DEFAULT_IRES = ErrorConstant.DV+" - "+ErrorConstant.IPRED;
	private static final String DEFAULT_IWRES = ErrorConstant.IRES+"/"+ErrorConstant.W;
	
	abstract StringBuilder getErrorStatement();
	
	ErrorType(String error){
		this.error = error;	
	}
	
	public String getErrorType() {
		return this.error;
	}
	
	/**
	 * Adds new statement and if new statement is not provided then uses default statement for function variable definition.
	 * 
	 * @param newStatement
	 * @param defaultStatement
	 * @return
	 */
	private static String addStatement(String newStatement, final String defaultStatement){
		String statement = (newStatement==null || newStatement.isEmpty())?defaultStatement:newStatement;
		return Formatter.endline(statement);
	}

	/**
	 * Frequently used method as only Weight has different definition in most of the functions.
	 * 
	 * @param weight
	 * @return
	 */
	private static StringBuilder createErrorStatement(String weight){
		return createErrorStatement(null, weight, null, null, null);
	}
	
	/**
	 * This method takes definitions of the error model variables 
	 * and if no definition is provided then uses default definition.
	 * It returns collective error model function definitions.
	 * @param iPred
	 * @param weight
	 * @param yStatement
	 * @param iRes
	 * @param iWRes
	 * @return
	 */
	private static StringBuilder createErrorStatement(String iPred, String weight, 
			String yStatement, String iRes, String iWRes){
		StringBuilder errorModel = new StringBuilder();

		DEFAULT_IPRED = ErrorStatement.functionRep;
		errorModel.append(ErrorConstant.IPRED	+" = "+ addStatement(iPred,DEFAULT_IPRED));
		errorModel.append(ErrorConstant.W		+" = "+ addStatement(weight,DEFAULT_WEIGHT));
		errorModel.append(ErrorConstant.Y		+" = "+ addStatement(yStatement,DEFAULT_Y));
		errorModel.append(ErrorConstant.IRES	+" = "+ addStatement(iRes,DEFAULT_IRES));
		errorModel.append(ErrorConstant.IWRES	+" = "+ addStatement(iWRes,DEFAULT_IWRES));
		
		return errorModel;
	}
}
