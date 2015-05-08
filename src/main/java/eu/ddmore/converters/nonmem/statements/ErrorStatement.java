/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.Map;
import java.util.regex.Pattern;

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

    private FunctionCallType functionCall = null;
    private String function = new String();
    private String errorType = new String();

    public ErrorStatement(FunctionCallType functionCallType, String output){
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

    public StringBuilder getErrorStatementDetails(){
        StringBuilder errorBlock = new StringBuilder();
        for(ErrorType error : ErrorType.values()){
            if(errorType.equals(error.getErrorType())){
                errorBlock.append(error.getErrorStatement());
                break;
            }
        }
        return errorBlock;
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
    public StringBuilder getDetailsForDES(Map<String,String> functionDefEqMap, Map<String,String> derivativeVarMap){
        StringBuilder errorBlock = new StringBuilder();
        //This could be null or empty in case of non-derivative
        errorBlock.append(getDerivativeVarDetails(functionDefEqMap, derivativeVarMap));
        errorBlock.append(getErrorStatementDetails());

        return errorBlock;	
    }

    private StringBuilder getDerivativeVarDetails(Map<String, String> functionDefEqMap, Map<String, String> derivativeVarMap) {
        StringBuilder errorBlock = new StringBuilder();
        if(functionDefEqMap!=null){
            if(functionDefEqMap.containsKey(function)){
                if(derivativeVarMap.containsKey(function)){
                    String varAmount = DiffEquationStatementBuilder.getVarAmountFromCompartment(function, derivativeVarMap);
                    functionRep = (varAmount.isEmpty())?function:varAmount;
                }else{
                    String functionEquation= getEquationForFunctionName(functionDefEqMap, derivativeVarMap);
                    errorBlock.append(Formatter.endline(function+" = "+functionEquation));
                }
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
                    String varAmount = DiffEquationStatementBuilder.getVarAmountFromCompartment(variable, derivativeVarMap);
                    if(!varAmount.isEmpty()){
                        String varToReplace = "\\b"+Pattern.quote(variable)+"\\b";
                        parsedEquation = parsedEquation.replaceAll(varToReplace, varAmount);
                    }
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
