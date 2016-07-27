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

import eu.ddmore.converters.nonmem.statements.error.StructuralObsErrorStatement.ErrorConstant;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Error statement emmitter uses structural observation error statement properties and create error statement for error type specified.
 */
public class StructuralObsErrorStatementEmitter {
    private final StructuralObsErrorStatement error;
    private final String DEFAULT_Y = ErrorConstant.IPRED+"+"+ErrorConstant.W;
    private final String DEFAULT_WEIGHT = " "+ErrorConstant.W;
    private final String DEFAULT_IRES = ErrorConstant.DV+" - "+ErrorConstant.IPRED;
    private final String DEFAULT_IWRES = ErrorConstant.IRES+"/"+ErrorConstant.W;

    public StructuralObsErrorStatementEmitter(ErrorStatement error){
        Preconditions.checkNotNull(error, "Error statement cannot be null");
        Preconditions.checkArgument(error.isStructuralObsError(), "Error statement should be structural observation error type");
        this.error = (StructuralObsErrorStatement) error;
    }

    /**
     * Gets error statement details with help of error type specified.
     *
     * @return {@link StringBuilder} containing the text for the error statement block
     */
    public StringBuilder getErrorStatementDetails() {
        final StructuralObsErrorType errorType = StructuralObsErrorType.fromErrorType(error.getErrorType());
        switch (errorType) {

        case COMBINED_ERROR_1 :
            return createErrorStatement(
                error.getAdditive() + "+" + error.getProportional() + "*" + ErrorConstant.IPRED);

        case COMBINED_ERROR_2 :
        {
            final String weight = "SQRT(("+error.getAdditive() + "*" +
                    error.getAdditive() + ")" + "+ (" + error.getProportional() + "*" +
                    error.getProportional() + "*" + ErrorConstant.IPRED + "*" + ErrorConstant.IPRED+"))";
            return createErrorStatement(weight);
        }

        case COMBINED_ERROR_3 :
        case COMBINED_ERROR_2_LOG :
        {
            final String iPred = "LOG(" + error.getFunctionName() + ")";
            final String weight = "SQRT(" + error.getProportional() + "**2" +
                    " + (" + error.getAdditive() + "/" + error.getFunctionName() + ")**2)";
            return createErrorStatement(iPred, weight);
        }

        case ADDITIVE_ERROR :
            return createErrorStatement(error.getAdditive());

        case PROPORTIONAL_ERROR :
            return createErrorStatement(error.getProportional() + "*" + ErrorConstant.IPRED);

        default :
            throw new UnsupportedOperationException("Unhandled ErrorType encountered in ErrorStatementEmitter");
        }
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
        return createErrorStatement(null, weight);
    }

    /**
     * This method takes definitions of the error model variables 
     * and if no definition is provided then uses default definition.
     * It returns collective error model function definitions.
     * @param iPred
     * @param weight
     * @return error model
     */
    private StringBuilder createErrorStatement(String iPred, String weight){
        StringBuilder errorModel = new StringBuilder();
        iPred = ((iPred!=null))?Formatter.getReservedParam(iPred):Formatter.getReservedParam(error.getFunctionName());
        weight = ((weight!=null))?Formatter.getReservedParam(weight):weight;

        errorModel.append(ErrorConstant.IPRED   +" = "+ addStatement(iPred,error.getFunctionName()));
        errorModel.append(ErrorConstant.W       +" = "+ addStatement(weight,DEFAULT_WEIGHT));
        errorModel.append(ErrorConstant.Y       +" = "+ Formatter.endline(DEFAULT_Y+"*"+error.getEpsilonVariable()));
        errorModel.append(ErrorConstant.IRES    +" = "+ Formatter.endline(DEFAULT_IRES));
        errorModel.append(ErrorConstant.IWRES   +" = "+ Formatter.endline(DEFAULT_IWRES));

        return errorModel;
    }
}
