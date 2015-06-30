/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.lang.reflect.Method;
import java.math.BigInteger;

import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;

/**
 * This class helps to get value from scalar objects.
 */
public final class ScalarValueHandler {

    /**
     * Gets variable from scalar rhs if it exists or else looks for scalar value and returns in string form.
     *    
     * @param rhs
     * @return scalar variable or value
     */
    public static String getVariableOrValueFromScalarRhs(ScalarRhs rhs) {
        String coefficient;
        if(rhs.getSymbRef()!=null){
            coefficient = rhs.getSymbRef().getSymbIdRef();
        }
        else{
            coefficient = getValueFromScalarRhs(rhs).toString();
        }
        return coefficient;
    }
    
    /**
     * Accepts scalar rhs object and looks for value in either scalar or scalar in equation.
     * 
     * @param rhs
     * @return
     */
    public static Double getValueFromScalarRhs(ScalarRhs rhs) {
        if(rhs.getScalar()!=null){
            return getValue(rhs.getScalar().getValue());
        }else if(rhs.getEquation().getScalar()!=null){
            return getValue(rhs.getEquation().getScalar().getValue());
        }else if(rhs.getEquation().getUniop()!=null){
            return getValue(rhs.getEquation().getUniop().getValue());
        }else{
            throw new IllegalArgumentException("Scalar value doesn't exist as expected.");
        }
    }
    
    /**
     * This method will accept scalar value object and retrievs value in form of double.
     *  
     * @param object
     * @return
     */
    public static Double getValue(Object object) {
        try {
            Method handler = ScalarValueHandler.class.getMethod("getValue", object.getClass());
            return (Double)handler.invoke(null, object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Double getValue(IntValue num) {
        BigInteger number = num.getValue();
        return number.doubleValue();
    }
    public static Double getValue(RealValue num) {
        return num.getValue();
    }
}
