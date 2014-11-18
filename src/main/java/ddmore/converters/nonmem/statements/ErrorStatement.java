package ddmore.converters.nonmem.statements;

import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;

public class ErrorStatement {
	
	String errorType = new String();
	String additive = new String();
	String proportional = new String();
	String func = new String();
	FunctionCallType functionCall = null;
	
	static final String COMBINED_ERROR_1 = "combinedError1";
	static final String COMBINED_ERROR_2 = "combinedError2";
	final String ADDITIVE = "additive";
	final String PROP = "proportional";
	final String FUNC = "f";
	
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
			if(arg.getSymbId().equals(ADDITIVE)){
				additive = arg.getSymbRef().getSymbIdRef();					
			}else if(arg.getSymbId().equals(PROP)){
				proportional = arg.getSymbRef().getSymbIdRef();
			}else if(arg.getSymbId().equals(FUNC)){
				func = arg.getSymbRef().getSymbIdRef();
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
	 *  	IPRED = <f> 
	 *  	IRES = DV - IPRED 
	 *  	IWRES = IRES / W
	 *  	where Additive is THETA(x), proportional is THETA(y) and f is <f>
	 *  
	 * @return
	 */
	public StringBuilder getErrorStatementDetails(){
		StringBuilder errorBlock = new StringBuilder();

		func = "NM_"+func;
		errorBlock.append("\nIPRED = "+func);
		
		// Simplified as we have details of two error types at this moment. 
		// This might need updates depending upon further error types details.
		if(errorType.equals(COMBINED_ERROR_1)){
			errorBlock.append("\nW = "+additive+"+"+proportional+"*IPRED");
		}else if(errorType.equals(COMBINED_ERROR_2)){
			errorBlock.append("\nW = SQRT(("+additive+"*"+additive+")"+"+ ("+proportional+"*"+proportional+"*IPRED*IPRED))");
		}
		errorBlock.append("\nY = "+func+"+W*EPS(1)");
		errorBlock.append("\nIPRED = "+func);
		errorBlock.append("\nIRES = DV - IPRED");
		errorBlock.append("\nIWRES = IRES / W\n");
		
		return errorBlock;	
	}

}
