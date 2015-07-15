/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.statements.Parameter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;

/**
 * This class deals with handling and formatting of parameter statements. 
 */
public class ParameterStatementHandler {

    /**
     * Prepares Theta and omega parameters according to the initial estimates, lower and upper bounds provided.
     * 
     * @param param
     * @return parameter statement
     */
    public static StringBuilder addParameter(Parameter param) {
        Preconditions.checkNotNull(param, "Paramter cannot be null");
        StringBuilder statement = new StringBuilder();
        String description = param.getSymbId().toUpperCase();

        ScalarRhs lowerBound = param.getLowerBound();
        ScalarRhs upperBound= param.getUpperBound(); 
        ScalarRhs initEstimate= param.getInitialEstimate();
        statement.append("(");
        statement.append(prepareParameterStatements(description, lowerBound, upperBound,initEstimate));

        if(param.isFixed()){
            statement.append(" "+NmConstant.FIX+" ");
        }
        if(param.isStdDev()){
            statement.append(NmConstant.SD+" ");
        }
        statement.append(Formatter.endline(")"+Formatter.indent(Symbol.COMMENT+description)));

        return statement;
    }

    /**
     *  Writes parameter statement as described in following table,
     *  
     *  LB  IN  UB  Action expected
     *  X   X   X   FAIL
     *  X   X   Y   FAIL
     *  X   Y   X   (IN)
     *  Y   X   X   FAIL
     *  X   Y   Y   (-INF,IN,UB)
     *  Y   Y   X   (LB,IN)
     *  Y   X   Y   (LB, ,UB)
     *  Y   Y   Y   (LB,IN,UB) 
     * 
     * @param description
     * @param lowerBound
     * @param upperBound
     * @param initEstimate
     * @return parameter statement
     */
    private static StringBuilder prepareParameterStatements(String description,
            ScalarRhs lowerBound, ScalarRhs upperBound, ScalarRhs initEstimate) {

        StringBuilder statement = new StringBuilder(); 
        if(lowerBound!=null){
            if(initEstimate!=null){
                if(upperBound!=null){
                    statement.append(prepareStatement(lowerBound,initEstimate,upperBound));
                }else{
                    statement.append(prepareStatement(lowerBound,initEstimate,null));
                }
            }else{
                if(upperBound!=null){
                    statement.append(prepareStatement(lowerBound,null,upperBound));
                }else{
                    throw new IllegalStateException("Only lower bound value present for parameter : "+description);
                }
            }
        }else if(initEstimate!=null){
            if(upperBound!=null){
                statement.append("-INF,");
                statement.append(prepareStatement(null,initEstimate,upperBound));
            }else{
                statement.append(prepareStatement(null,initEstimate,null));
            }
        }else {
            throw new IllegalStateException("Only upper bound or no values present for parameter : "+description);
        }

        return statement;
    }

    /**
     * Writes bound values of a parameter statement in expected format.
     * 
     * @param lowerBound
     * @param init
     * @param upperBound
     * @return parameter statement
     */
    private static StringBuilder prepareStatement(ScalarRhs lowerBound,ScalarRhs init,ScalarRhs upperBound){
        StringBuilder statement = new StringBuilder();
        Double value;
        if(lowerBound!=null){
            value = ScalarValueHandler.getValueFromScalarRhs(lowerBound);
            statement.append(" "+value+" , ");
        }
        if(init!=null){
            value = ScalarValueHandler.getValueFromScalarRhs(init);
            statement.append(value+" ");
        }
        if(upperBound!=null){
            value = ScalarValueHandler.getValueFromScalarRhs(upperBound);
            statement.append(", "+value+" ");
        }
        return statement;
    }

}
