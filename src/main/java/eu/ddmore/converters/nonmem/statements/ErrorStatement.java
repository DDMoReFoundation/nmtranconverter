/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
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

    private String additive = new String("0.0");
    private String proportional = new String("0.0");
    private String functionRep = new String();

    private FunctionCallType functionCall = null;
    private String function = new String();
    private String errorType = new String();

    public ErrorStatement(FunctionCallType functionCallType, String output){
        if(functionCallType!=null){
            functionCall = functionCallType;
            setParamsFunctionCall(output);
            functionRep = function;
        }
    }

    /**
     * This method will set additive, proportional and f values required to create error statement.	
     */
    private void setParamsFunctionCall(String output){
        errorType = functionCall.getSymbRef().getSymbIdRef();
        for(FunctionArgument arg : functionCall.getFunctionArgument()){
            String paramValue = fetchParamValue(arg);
            if(arg.getSymbId()!=null && paramValue!=null){
                
                if(arg.getSymbId().equals(FunctionArg.ADDITIVE.getDescription())){
                    additive = (paramValue.isEmpty())?additive:paramValue;
                    
                }else if(arg.getSymbId().equals(FunctionArg.PROP.getDescription())){
                    proportional = (paramValue.isEmpty())?proportional:paramValue;
                    
                }else if(arg.getSymbId().equals(FunctionArg.FUNC.getDescription())){
                    function = (paramValue.isEmpty())?output:paramValue;
                }
            }
        }
    }

    /**
     * We need to determine parameter value from function argument to be added to nmtran. 
     * It could be either variable name or value provided.
     * 
     * @param arg
     * @return
     */
    private String fetchParamValue(FunctionArgument arg) {
        String paramValue = new String();
        if(arg.getSymbRef()!=null){
            paramValue = arg.getSymbRef().getSymbIdRef();
        }else if(arg.getScalar()!=null){
            paramValue = ScalarValueHandler.getValue(arg.getScalar().getValue()).toString();
        }
        return paramValue;
    }

    public String getFunction() {
        return function;
    }

    public String getAdditive() {
        return additive;
    }

    public String getProportional() {
        return proportional;
    }

    public String getFunctionRep() {
        return functionRep;
    }

    public FunctionCallType getFunctionCall() {
        return functionCall;
    }

    public String getErrorType() {
        return errorType;
    }
}
