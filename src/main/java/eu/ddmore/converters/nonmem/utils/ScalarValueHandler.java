/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
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
    public static Double getValueFromScalarRhs(Rhs rhs) {
        Preconditions.checkNotNull(rhs, "Scalar rhs cannot be null");
        if(rhs.getScalar()!=null){
            return Double.parseDouble(rhs.getScalar().valueToString());
        }else if(rhs.getUniop()!=null){
            return getUniopValue(rhs.getUniop());
        } else if(rhs.getBinop()!=null) {
            return ExpressionEvaluator.evaluate(rhs.getBinop().toMathExpression());
        } else{
            throw new IllegalArgumentException(String.format("The expected scalar value doesn't exist %s [id = %s].", rhs, rhs.getId()));
        }
    }

    private static Double getUniopValue(Uniop uniop){
        if(uniop.getValue() instanceof Binop){
            throw new IllegalStateException("Binary Operation not supported as uniop of rhs at the moment. ");
        }
        Properties uniopProperties = loadUniopProperties();

        String operator = uniop.getOperator().getOperator();
        String op = new String();

        if(!operator.isEmpty() && uniopProperties.stringPropertyNames().contains(operator)){
            op = uniopProperties.getProperty(operator);
        }
        Scalar value = (Scalar) uniop.getValue();
        return Double.parseDouble(op+value.valueToString());
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
        Preconditions.checkNotNull(object, "The object value cannot be null");
        try {
            Method handler = ScalarValueHandler.class.getMethod("getValue", object.getClass());
            return (Double)handler.invoke(null, object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Double getValue(IntValue num) {
        Preconditions.checkNotNull(num, "The integer value cannot be null");
        Integer number = num.getValue();
        return number.doubleValue();
    }

    public static Double getValue(RealValue num) {
        Preconditions.checkNotNull(num, "The real value cannot be null");
        return num.getValue();
    }
}
