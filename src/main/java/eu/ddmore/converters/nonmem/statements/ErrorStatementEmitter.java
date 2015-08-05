/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.statements.ErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;


public class ErrorStatementEmitter {
    ErrorStatement error;
    private final String DEFAULT_Y = ErrorConstant.IPRED+"+"+ErrorConstant.W+"*EPS(1)";
    private final String DEFAULT_WEIGHT = " "+ErrorConstant.W;
    private final String DEFAULT_IRES = ErrorConstant.DV+" - "+ErrorConstant.IPRED;
    private final String DEFAULT_IWRES = ErrorConstant.IRES+"/"+ErrorConstant.W;

    public ErrorStatementEmitter(ErrorStatement errorStatement){
        Preconditions.checkNotNull(errorStatement, "Error statement cannot be null");
        this.error = errorStatement; 
    }

    /**
     * Gets error statement details with help of error type specified.
     *
     * @return
     */
    public StringBuilder getErrorStatementDetails(){
        StringBuilder errorBlock = new StringBuilder();
        String errorType = error.getErrorType();

        if(errorType.equals(ErrorType.COMBINED_ERROR_1.getErrorType())){
            String weight = error.getAdditive()+
                    "+"+error.getProportional()+"*"+ErrorConstant.IPRED;
            return createErrorStatement(weight);

        }else if(errorType.equals(ErrorType.COMBINED_ERROR_2.getErrorType())){
            String weight = "SQRT(("+error.getAdditive()+"*"+
                    error.getAdditive()+")"+"+ ("+error.getProportional()+"*"+
                    error.getProportional()+"*"+ErrorConstant.IPRED+"*"+ErrorConstant.IPRED+"))";
            return createErrorStatement(weight);

        }else if(errorType.equals(ErrorType.COMBINED_ERROR_3.getErrorType()) 
                || errorType.equals(ErrorType.COMBINED_ERROR_2_LOG.getErrorType())){
            String iPred = "LOG("+error.getFunctionRep()+")";
            String weight = "SQRT("+error.getProportional()+"**2"+
                    " + ("+error.getAdditive()+"/"+error.getFunctionRep()+")**2)";
            return createErrorStatement(iPred, weight, null, null, null);

        }else if(errorType.equals(ErrorType.ADDITIVE_ERROR.getErrorType())){
            String weight = error.getAdditive();
            return createErrorStatement(weight);

        }else if(errorType.equals(ErrorType.PROPORTIONAL_ERROR.getErrorType())){
            String weight = error.getProportional()+"*"+ErrorConstant.IPRED;
            return createErrorStatement(weight);
        }
        return errorBlock;
    }

    /**
     * Adds new statement and if new statement is not provided then uses default statement for function variable definition.
     * 
     * @param newStatement
     * @param defaultStatement
     * @return
     */
    private String addStatement(String newStatement, final String defaultStatement){
        String statement = (newStatement==null || newStatement.isEmpty())?defaultStatement:newStatement;
        return Formatter.endline(statement);
    }

    /**
     * Frequently used method as only Weight has different definition in most of the functions.
     * 
     * @param weight
     * @return
     */
    private StringBuilder createErrorStatement(String weight){
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
    private StringBuilder createErrorStatement(String iPred, String weight, 
            String yStatement, String iRes, String iWRes){
        StringBuilder errorModel = new StringBuilder();

        errorModel.append(ErrorConstant.IPRED   +" = "+ addStatement(iPred,error.getFunctionRep()));
        errorModel.append(ErrorConstant.W       +" = "+ addStatement(weight,DEFAULT_WEIGHT));
        errorModel.append(ErrorConstant.Y       +" = "+ addStatement(yStatement,DEFAULT_Y));
        errorModel.append(ErrorConstant.IRES    +" = "+ addStatement(iRes,DEFAULT_IRES));
        errorModel.append(ErrorConstant.IWRES   +" = "+ addStatement(iWRes,DEFAULT_IWRES));

        return errorModel;
    }
}
