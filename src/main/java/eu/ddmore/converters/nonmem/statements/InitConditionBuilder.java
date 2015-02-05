/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.List;

import javax.xml.bind.JAXBElement;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariableType;
import eu.ddmore.libpharmml.dom.commontypes.InitialValueType;
import eu.ddmore.libpharmml.dom.commontypes.IntValueType;
import eu.ddmore.libpharmml.dom.commontypes.RealValueType;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRefType;
import eu.ddmore.libpharmml.dom.maths.Equation;

public class InitConditionBuilder {
	
	static Integer derivativeVarCount = 0;

	/**
	 * Creates DES statement block from differential initial conditions.
	 * 
	 * @return
	 */	
	public static StringBuilder getDifferentialInitialConditions(List<StructuralBlock> structuralBlocks){
		
		StringBuilder builder = new StringBuilder();
		for(StructuralBlock structBlock : structuralBlocks){
			derivativeVarCount = 1;
			for(DerivativeVariableType variableType : structBlock.getStateVariables()){
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
	private static StringBuilder getInitConditionFromCompartment(DerivativeVariableType variableType) {
		StringBuilder builder = new StringBuilder();
		if(variableType.getInitialCondition()!=null){
			InitialValueType initialValueType = variableType.getInitialCondition().getInitialValue();
			
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
	private static String getNmTranSpecificInitCondition(SymbolRefType symbolRef, JAXBElement<?> scalar) {
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
	private static String getInitConditionFromSymbRef(SymbolRefType initConditionVar) {
		if(derivativeVarCount==0 || initConditionVar.getSymbIdRef() == null){
			throw new IllegalStateException("Could not get initial condition for The derivative variable");
		}
		return Formatter.endline("A_0("+derivativeVarCount+") = "
				+Formatter.addPrefix(initConditionVar.getSymbIdRef().toUpperCase()));
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
		if(value instanceof RealValueType){
			initialCondition = Double.toString(((RealValueType)value).getValue());
		} else if(value instanceof IntValueType){
			initialCondition = ((IntValueType)value).getValue().toString();
		}
		return Formatter.endline("A_0("+derivativeVarCount+") = "
				+Formatter.addPrefix(initialCondition.toUpperCase()));
	}
	
	
}