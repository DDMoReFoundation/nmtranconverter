/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.InitialValue;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.maths.Equation;

public class InitConditionBuilder {

    private static Integer derivativeVarCount = 0;

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return
     */	
    public static StringBuilder getDifferentialInitialConditions(List<StructuralBlock> structuralBlocks){

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
    private static StringBuilder getInitConditionFromCompartment(DerivativeVariable variableType) {
        StringBuilder builder = new StringBuilder();
        if(variableType.getInitialCondition()!=null){
            InitialValue initialValueType = variableType.getInitialCondition().getInitialValue();

            if(initialValueType!=null){
                Rhs assign = initialValueType.getAssign();

                String initialCondition = getNmTranSpecificInitCondition(assign.getSymbRef(), assign.getScalar());
                if(initialCondition.isEmpty()) {
                    Equation equation = assign.getEquation();
                    initialCondition = getNmTranSpecificInitCondition(equation.getSymbRef(), equation.getScalar());
                }
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
    private static String getNmTranSpecificInitCondition(SymbolRef symbolRef, JAXBElement<?> scalar) {
        if(symbolRef!=null){
            return getInitConditionFromSymbRef(symbolRef);
        }
        else if(scalar!=null){
            return getInitConditionFromScalar(scalar.getValue());
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
    private static String getInitConditionFromSymbRef(SymbolRef initConditionVar) {
        if(derivativeVarCount==0 || initConditionVar.getSymbIdRef() == null){
            throw new IllegalStateException("Could not get initial condition for The derivative variable");
        }
        return Formatter.endline("A_0("+derivativeVarCount+") = "+initConditionVar.getSymbIdRef().toUpperCase());
    }

    /**
     * Create nmtran initial condition representation from scalar
     * 
     * @param derivativeVarCount
     * @param value
     * @return
     */
    private static String getInitConditionFromScalar(Object value) {
        if(derivativeVarCount==0 || value == null){
            throw new IllegalStateException("Could not get initial condition for The derivative variable");
        }
        String initialCondition = new String();
        if(value instanceof RealValue){
            initialCondition = Double.toString(((RealValue)value).getValue());
        } else if(value instanceof IntValue){
            initialCondition = ((IntValue)value).getValue().toString();
        }
        return Formatter.endline("A_0("+derivativeVarCount+") = "+initialCondition.toUpperCase());
    }


}