/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Properties;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.Uniop;

/**
 * This class helps to get value from scalar objects.
 */
public final class ScalarValueHandler {

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
            Uniop uniop = rhs.getEquation().getUniop();
            return getUniopValue(uniop);
        }else{
            throw new IllegalArgumentException("Scalar value doesn't exist as expected.");
        }
    }
    
    private static Double getUniopValue(Uniop uniop){
        if(uniop.getValue() instanceof Binop){
            throw new IllegalStateException("Binary Operation not supported as uniop of scalar rhs at the moment. ");
        }
        Properties uniopProperties = loadUniopProperties();
        
        String operator = uniop.getOperator().getOperator();
        String op = new String();
        
        if(!operator.isEmpty() && uniopProperties.stringPropertyNames().contains(operator)){
            op = uniopProperties.getProperty(operator);
        }
        return Double.parseDouble(op+getValue(uniop.getValue()));
    }

    private static Properties loadUniopProperties() {
        Properties binopProperties = new Properties();
        try {
            binopProperties.load(ConversionContext.class.getResourceAsStream("unary_operators_list.txt"));
        } catch (IOException e) {
            throw new IllegalStateException("Binary Operation not recognised : "+ e);
        }
        return binopProperties;
    }

    /**
     * This method will accept scalar value object and retrieves value in form of double.
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
