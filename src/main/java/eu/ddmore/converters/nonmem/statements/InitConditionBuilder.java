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
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

public class InitConditionBuilder {

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return
     */	
    public StringBuilder getDifferentialInitialConditions(ConversionContext context){
        StringBuilder builder = new StringBuilder();

        for(DerivativeVariable variableType : context.getDerivativeVars()){
            String variable = variableType.getSymbId().toUpperCase();
            Integer variableOrder = Integer.parseInt(context.getDerivativeVarCompSequences().get(variable));
            builder.append(getInitConditionFromCompartment(variableType, variableOrder));
        }

        builder.append(Formatter.endline());
        return builder;
    }

    /**
     * Gets intial condition from compartment for derivative variable.
     * 
     * @param derivativeVarCount
     * @param variableType
     * @param variableOrder 
     * @return
     */
    private StringBuilder getInitConditionFromCompartment(DerivativeVariable variableType, Integer variableOrder) {
        StringBuilder builder = new StringBuilder();
        if(variableType.getInitialCondition()!=null){
            StandardAssignable initialValueType = variableType.getInitialCondition().getInitialValue();

            if(initialValueType!=null){
                Rhs assign = initialValueType.getAssign();

                String initialCondition = getNmTranSpecificInitCondition(assign.getSymbRef(), assign.getScalar(), variableOrder);
                //  if(initialCondition.isEmpty()) {
                //      Equation equation = assign.getEquation();
                //      initialCondition = getNmTranSpecificInitCondition(equation.getSymbRef(), equation.getScalar());
                //  }
                builder.append(initialCondition);
            }
        }
        return builder;
    }

    /**
     * Create nmtran initial condition for Rhs or equation type.
     * This method accepts symbref and scalar as params and returns empty string if no representation is found.
     * 
     * @param derivativeVarCount
     * @param symbolRef
     * @param scalar
     * @param derivativeVarCount 
     * @return
     */
    private String getNmTranSpecificInitCondition(SymbolRef symbolRef, Scalar scalar, Integer derivativeVarCount) {
        if(symbolRef!=null){
            return getInitConditionFromSymbRef(symbolRef, derivativeVarCount);
        }
        else if(scalar!=null){
            return Formatter.endline("A_0("+derivativeVarCount+") = "+scalar.valueToString().toUpperCase());
        }else {
            return new String();
        }
    }

    /**
     * Create nmtran initial condition representation from symbref
     * 
     * @param derivativeVarCount
     * @param initConditionVar
     * @param derivativeVarCount 
     * @return
     */
    private String getInitConditionFromSymbRef(SymbolRef initConditionVar, Integer derivativeVarCount) {
        if(derivativeVarCount==0 || initConditionVar.getSymbIdRef() == null){
            throw new IllegalStateException("Could not get initial condition for The derivative variable");
        }
        return Formatter.endline("A_0("+derivativeVarCount+") = "+initConditionVar.getSymbIdRef().toUpperCase());
    }
}