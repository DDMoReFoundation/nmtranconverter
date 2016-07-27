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
package eu.ddmore.converters.nonmem.parameters;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;

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
        String description = param.getSymbId();

        Rhs lowerBound = param.getLowerBound();
        Rhs upperBound= param.getUpperBound(); 
        Rhs initEstimate= param.getInitialEstimate();
        statement.append("(");

        if(param.isFixed()){
            statement.append(prepareParameterStatements(description, null, null,initEstimate)+" "+NmConstant.FIX+" ");
        }else {
            statement.append(prepareParameterStatements(description, lowerBound, upperBound,initEstimate));
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
            Rhs lowerBound, Rhs upperBound, Rhs initEstimate) {

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
    private static StringBuilder prepareStatement(Rhs lowerBound,Rhs init,Rhs upperBound){
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
