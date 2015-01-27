package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.converters.nonmem.statements.ErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;

public enum ErrorType {
	COMBINED_ERROR_1("combinedError1"){
		@Override
		String getStatement() {
			return Formatter.endline("W = "+ErrorStatement.additive+
					"+"+ErrorStatement.proportional+"*"+ErrorConstant.IPRED);
		};
	},
	COMBINED_ERROR_2("combinedError2"){
		@Override
		String getStatement() {
			return Formatter.endline("W = SQRT(("+ErrorStatement.additive+"*"+
					ErrorStatement.additive+")"+"+ ("+ErrorStatement.proportional+"*"+
					ErrorStatement.proportional+"*"+ErrorConstant.IPRED+"*"+ErrorConstant.IPRED+"))");
		};
	},
	ADDITIVE_ERROR("additiveError"){
		@Override
		String getStatement() {
			return Formatter.endline("W = "+ErrorStatement.additive);
		};
	},
	PROPORTIONAL_ERROR("proportionalError"){
		@Override
		String getStatement() {
			return Formatter.endline("W = "+ErrorStatement.proportional+"*"+ErrorConstant.IPRED);
		};
	};
	
	String error;
	ErrorType(String error){
		this.error = error;	
	}
	public String getErrorType() {
		return this.error;
	}
	abstract String getStatement();
}
