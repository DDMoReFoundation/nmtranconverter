package eu.ddmore.converters.nonmem.statements;

import java.util.Map;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;

public class ErrorStatement {

	public static final String IWRES = "IWRES";

	public static final String IRES = "IRES";

	public static final String IPRED = "IPRED";
	
	public static final String Y = "Y";

	String errorType, additive, proportional, function, functionEquation;

	FunctionCallType functionCall = null;
	
	static final String COMBINED_ERROR_1 = "combinedError1";
	static final String COMBINED_ERROR_2 = "combinedError2";
	final String ADDITIVE = "additive";
	final String PROP = "proportional";
	final String FUNC = "f";
	final String ERROR_VAR_SUFFIX = "_ERR";
	
	ErrorStatement(FunctionCallType functionCallType){
		if(functionCallType!=null){
			functionCall = functionCallType;
			setParamsFunctionCall();
		}
	}
	
	/**
	 * This method will set additive, proportional and f values required to create error statement.	
	 */
	private void setParamsFunctionCall(){
		errorType = functionCall.getSymbRef().getSymbIdRef();
		for(FunctionArgument arg : functionCall.getFunctionArgument()){
			String param = arg.getSymbRef().getSymbIdRef();
			if(arg.getSymbId()!=null && param!=null){
				if(arg.getSymbId().equals(ADDITIVE)){
					additive = Formatter.addPrefix(param);					
				}else if(arg.getSymbId().equals(PROP)){
					proportional = Formatter.addPrefix(param);
				}else if(arg.getSymbId().equals(FUNC)){
					function = Formatter.addPrefix(param);
				}
			}
		}		
	}
	
	/**
	 * Creates error statement details depending upon error type specified,
	 * e.g. : 
	 * if error type is CombinedError1 then,
	 * 		IPRED = <f>  	 
	 *  	W = THETA(x)+THETA(y)*IPRED 
	 *  	Y = <f>+W*EPS(z) 
	 *  	IRES = DV - IPRED 
	 *  	IWRES = IRES / W
	 *  	where Additive is THETA(x), proportional is THETA(y) and f is <f>
	 *  
	 * @return
	 */
	public StringBuilder getErrorStatementDetails(Map<String,String> defParsingMap, Map<String,String> derivativeVarMap){
		StringBuilder errorBlock = new StringBuilder();
		if(defParsingMap.containsKey(function)){
			functionEquation= getEquationForFunctionName(defParsingMap, derivativeVarMap);
			function = renameFunctionNameDefinedInDES(defParsingMap);
			errorBlock.append(Formatter.endline("\n"+function+" = "+functionEquation));
		}
		
		errorBlock.append(Formatter.endline(IPRED+" = "+function));
		
		// Simplified as we have details of two error types at this moment. 
		// This might need updates depending upon further error types details.
		if(errorType.equals(COMBINED_ERROR_1)){
			errorBlock.append(Formatter.endline("W = "+additive+"+"+proportional+"*"+IPRED));
		}else if(errorType.equals(COMBINED_ERROR_2)){
			errorBlock.append(Formatter.endline("W = SQRT(("+additive+"*"+additive+")"+"+ ("+proportional+"*"+proportional+"*"+IPRED+"*"+IPRED+"))"));
		}
		errorBlock.append(Formatter.endline(Y+" = "+function+"+W*EPS(1)"));
		errorBlock.append(Formatter.endline(IRES+" = DV - "+IPRED));
		errorBlock.append(Formatter.endline(IWRES+" = "+IRES+"/ W"));
		
		return errorBlock;	
	}
	
	public String getEquationForFunctionName(Map<String,String> defParsingMap, Map<String,String> derivativeVarMap){
		
		if(defParsingMap.containsKey(function)){
			String parsedEquation = defParsingMap.get(function);
			
			for(String variable: derivativeVarMap.keySet()){
				if(parsedEquation.contains(variable)){
					parsedEquation = parsedEquation.replaceAll(variable, "A("+derivativeVarMap.get(variable)+")");
				}
			}
			return parsedEquation;
		}
			return new String();
	}
	
	public String renameFunctionNameDefinedInDES(Map<String,String> defParsingMap){
		if(defParsingMap.containsKey(function)){			
			return function+ERROR_VAR_SUFFIX;
		}
		return function;
	}
	
	public String getFunction() {
		return function;
	}
	
	public void setFunction(String function) {
		this.function = function;
	}

}
