/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.error;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;

/**
 * Initialises and stores structural observation error statement and related information for nmtran
 */
public class StructuralObsErrorStatement extends ErrorStatement {

    public enum ErrorConstant{
        DV,	IWRES, IRES, IPRED, Y, W;
    }
    public enum FunctionArg{
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

    private FunctionCallType functionCallType = null;
    private String functionName = new String();
    private String errorType = new String();
    private String epsilonVariable = new String();

    public StructuralObsErrorStatement(FunctionCallType functionCallType, String output, String epsilonVar, boolean isStructuralObsError){
        super(isStructuralObsError);
        Preconditions.checkNotNull(functionCallType, "functional call type cannot be null");

        this.functionCallType = functionCallType;
        this.epsilonVariable = epsilonVar;
        initParamsFromFunctionDetails(output);
        populateErrorStatement();
    }

    /**
     * This method will set additive, proportional and f values required to create error statement.	
     */
    private void initParamsFromFunctionDetails(String output){
        errorType = functionCallType.getSymbRef().getSymbIdRef();
        for(FunctionArgument arg : functionCallType.getListOfFunctionArgument()){
            String paramValue = fetchParamValue(arg);
            if(arg.getSymbId()!=null && paramValue!=null){

                if(arg.getSymbId().equals(FunctionArg.ADDITIVE.getDescription())){
                    additive = (paramValue.isEmpty())?additive:paramValue;

                }else if(arg.getSymbId().equals(FunctionArg.PROP.getDescription())){
                    proportional = (paramValue.isEmpty())?proportional:paramValue;

                }else if(arg.getSymbId().equals(FunctionArg.FUNC.getDescription())){
                    functionName = paramValue;
                }
            }
        }
        if(functionName.isEmpty()){
            functionName = output;
        }
    }

    /**
     * We need to determine parameter value from functionName argument to be added to nmtran. 
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

    private void populateErrorStatement(){
        StructuralObsErrorStatementEmitter statementEmitter = new StructuralObsErrorStatementEmitter(this);
        super.setErrorStatement(statementEmitter.getErrorStatementDetails());
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getAdditive() {
        return additive;
    }

    public String getProportional() {
        return proportional;
    }

    public FunctionCallType getFunctionCallType() {
        return functionCallType;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getEpsilonVariable() {
        return epsilonVariable;
    }

    public void setEpsilonVariable(String epsilonVariable) {
        this.epsilonVariable = epsilonVariable;
    }
}
