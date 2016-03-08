/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;

public class InitConditionBuilder {

    private Integer derivativeVarCount = 0;

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return
     */	
    public StringBuilder getDifferentialInitialConditions(List<StructuralBlock> structuralBlocks){

        StringBuilder builder = new StringBuilder();
        for(StructuralBlock structBlock : structuralBlocks){
            derivativeVarCount = 1;
            for(DerivativeVariable variableType : structBlock.getStateVariables()){
                builder.append(getInitConditionFromCompartment(variableType));
                derivativeVarCount++;
            }
        }
        builder.append(Formatter.endline());
        return builder;
    }

    /**
     * Gets intial condition from compartment for derivative variable.
     * 
     * @param derivativeVarCount
     * @param variableType
     * @return
     */
    private StringBuilder getInitConditionFromCompartment(DerivativeVariable variableType) {
        StringBuilder builder = new StringBuilder();
        if(variableType.getInitialCondition()!=null){
            StandardAssignable initialValueType = variableType.getInitialCondition().getInitialValue();

            if(initialValueType!=null){
                Rhs assign = initialValueType.getAssign();

                String initialCondition = getNmTranSpecificInitCondition(assign.getSymbRef(), assign.getScalar());
//                if(initialCondition.isEmpty()) {
//                    Equation equation = assign.getEquation();
//                    initialCondition = getNmTranSpecificInitCondition(equation.getSymbRef(), equation.getScalar());
//                }
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
     * @return
     */
    private String getNmTranSpecificInitCondition(SymbolRef symbolRef, Scalar scalar) {
        if(symbolRef!=null){
            return getInitConditionFromSymbRef(symbolRef);
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
     * @return
     */
    private String getInitConditionFromSymbRef(SymbolRef initConditionVar) {
        if(derivativeVarCount==0 || initConditionVar.getSymbIdRef() == null){
            throw new IllegalStateException("Could not get initial condition for The derivative variable");
        }
        return Formatter.endline("A_0("+derivativeVarCount+") = "+initConditionVar.getSymbIdRef().toUpperCase());
    }
}