/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.handlers;

import java.io.IOException;
import java.util.Properties;

import eu.ddmore.converters.nonmem.Parser;

/**
 * This class loads properties and provides access to property values for users.
 */
public class PropertiesHandler {
    private static final Properties binopProperties = new Properties();
    private static final Properties reservedWordsProperties = new Properties();

    public PropertiesHandler(){
        loadBinopProperties();
        loadReservedWordsProperties();
    }

    /**
     * Loads binary operator properties from properties file. 
     * @return
     */
    private void loadBinopProperties() {

        try {
            binopProperties.load(Parser.class.getResourceAsStream("binary_operator.properties"));
        } catch (IOException e) {
            throw new IllegalStateException("Binary properties are not accesible : "+ e);
        }
    }

    /**
     * Loads binary operator properties from properties file. 
     * @return
     */
    private void loadReservedWordsProperties() {

        try {
            reservedWordsProperties.load(Parser.class.getResourceAsStream("reserved_words.txt"));
        } catch (IOException e) {
            throw new IllegalStateException("Binary properties are not accesible : "+ e);
        }
    }

    /**
     * Returns binary operator property for symbol provided. 
     * If property value doesn't exist for the symbol, then throws IllegalStateException.
     * @param symbol
     * @return
     */
    public String getBinopPropertyFor(String symbol) {
        String param = symbol.toLowerCase();
        String binop = getPropertyFor(binopProperties, param);
        if(symbol.equalsIgnoreCase(binop)){
            throw new IllegalStateException("Property value is not available for : "+ symbol);
        }else{
            return binop;
        }
    }

    /**
     * Returns reserved word replacement for symbol provided. 
     * If symbol provided is not on the reserved words list, then throws IllegalStateException.
     * @param symbol
     * @return
     */
    public String getReservedWordFor(String symbol) {
        String param = symbol.toUpperCase();
        return getPropertyFor(reservedWordsProperties, param);
    }
    
    private String getPropertyFor(Properties properties, String symbol){
        if(properties!=null && properties.stringPropertyNames().contains(symbol)){
            return properties.getProperty(symbol);
        }else{
            return symbol;
        }
    }
}
