package eu.ddmore.converters.nonmem.statements;

import java.util.Map;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;

public class ErrorStatement {
	
	enum ErrorConstant{
		DV,	IWRES, IRES, IPRED, Y, W;
	}
	enum FunctionArg{
		ADDITIVE ("additive"),
		PROP ("proportional"),
		FUNC ("f");
		
		String description;
		FunctionArg(String description){
			this.description = description;	
		}
		public String getDescription() {
			return this.description;
		}
	}

	static String additive = new String();
	static String proportional = new String();
	static String functionRep = new String();
	
	FunctionCallType functionCall = null;
	String function = new String();
	String errorType = new String();
	
	ErrorStatement(FunctionCallType functionCallType, String output){
		if(functionCallType!=null){
			functionCall = functionCallType;
			setParamsFunctionCall();
			if(function.isEmpty()){
				function = Formatter.addPrefix(output);
			}
			functionRep = function;
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
				if(arg.getSymbId().equals(FunctionArg.ADDITIVE.getDescription())){
					additive = Formatter.addPrefix(param);					
				}else if(arg.getSymbId().equals(FunctionArg.PROP.getDescription())){
					proportional = Formatter.addPrefix(param);
				}else if(arg.getSymbId().equals(FunctionArg.FUNC.getDescription())){					
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
	 *  	Y = IPRED+W*EPS(z) 
	 *  	IRES = DV - IPRED 
	 *  	IWRES = IRES / W
	 *  	where Additive is THETA(x), proportional is THETA(y) and f is <f>
	 *  
	 * @return
	 */
	public StringBuilder getErrorStatementDetails(Map<String,String> functionDefEqMap, Map<String,String> derivativeVarMap){
		StringBuilder errorBlock = new StringBuilder();
		if(functionDefEqMap.containsKey(function)){
			if(derivativeVarMap.containsKey(function)){
				String varAmount = PredStatement.getVarAmountFromCompartment(function, derivativeVarMap);
				if(!varAmount.isEmpty())
					functionRep = varAmount;
			}else{
				String functionEquation= getEquationForFunctionName(functionDefEqMap, derivativeVarMap);
				errorBlock.append(Formatter.endline(function+" = "+functionEquation));
			}
		}
		
		for(ErrorType error : ErrorType.values()){
			if(errorType.equals(error.getErrorType())){
				errorBlock.append(error.getErrorStatement());
				break;
			}
		}

		return errorBlock;	
	}
	
	/**
	 * This will return equation for the function name.
	 * 
	 * @param functionDefEqMap
	 * @param derivativeVarMap
	 * @return
	 */
	public String getEquationForFunctionName(Map<String,String> functionDefEqMap, Map<String,String> derivativeVarMap){
		if(functionDefEqMap.containsKey(function)){
			String parsedEquation = functionDefEqMap.get(function);
			for(String variable: derivativeVarMap.keySet()){
				if(parsedEquation.contains(variable)){
					String varAmount = PredStatement.getVarAmountFromCompartment(variable, derivativeVarMap);
					if(!varAmount.isEmpty())
						parsedEquation = parsedEquation.replaceAll(variable, varAmount);
				}
			}
			return parsedEquation;
		}
			return new String();
	}
	
	public String getFunction() {
		return function;
	}
}
