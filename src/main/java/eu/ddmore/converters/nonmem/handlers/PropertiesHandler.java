/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.handlers;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.Parser;

/**
 * This class loads properties and provides access to property values for users.
 */
public class PropertiesHandler {
    private static final String COLUMN_TYPE_PROPERTIES_FILE_NAME = "column_type.properties";
    private static final String RESERVED_WORDS__FILE_NAME = "reserved_words.txt";
    private static final String BINOP_PROPERTIES_FILE_NAME = "binary_operator.properties";

    private static final Properties binopProperties = new Properties();
    private static final Properties reservedWordsProperties = new Properties();
    private static final Properties columnTypeProperties = new Properties();

    public PropertiesHandler(){
        initialise();
    }

    private void initialise(){
        loadBinopProperties();
        loadReservedWordsProperties();
        loadColumnTypeProperties();
    }

    /**
     * Loads binary operator properties from properties file. 
     */
    private void loadColumnTypeProperties() {

        try {
            columnTypeProperties.load(Parser.class.getResourceAsStream(COLUMN_TYPE_PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            throw new IllegalStateException("Binary properties are not accesible : ", e);
        }
    }

    /**
     * Returns reserved word replacement for symbol provided. 
     * If symbol provided is not on the reserved words list, then throws IllegalStateException.
     * @param symbol
     * @return column name
     */
    public String getColumnNameForColumnType(String columnType) {
        String param = columnType.toUpperCase();
        return getPropertyFor(columnTypeProperties, param);
    }

    /**
     * Checks if column name is in the reserved columns list.
     * @param columnName
     * @return boolean flag if column is reserved 
     */
    public boolean isReservedColumnName(String columnName){
        return columnTypeProperties.containsValue(columnName);
    }

    /**
     * Loads binary operator properties from properties file. 
     */
    private void loadBinopProperties() {

        try {
            binopProperties.load(Parser.class.getResourceAsStream(BINOP_PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            throw new IllegalStateException("Binary properties are not accesible : ", e);
        }
    }

    /**
     * Returns binary operator property for symbol provided. 
     * If property value doesn't exist for the symbol, then throws IllegalStateException.
     * @param symbol
     * @return binop property
     */
    public String getBinopPropertyFor(String symbol) {
        Preconditions.checkNotNull(symbol, "symbol cannot be null");
        String param = symbol.toLowerCase();
        String binop = getPropertyFor(binopProperties, param);
        if(StringUtils.isEmpty(binop)){
            throw new IllegalStateException("Property value is not available for : "+ symbol);
        }else{
            return binop;
        }
    }

    /**
     * Loads binary operator properties from properties file. 
     */
    private void loadReservedWordsProperties() {

        try {
            reservedWordsProperties.load(Parser.class.getResourceAsStream(RESERVED_WORDS__FILE_NAME));
        } catch (IOException e) {
            throw new IllegalStateException("Binary properties are not accesible : ", e);
        }
    }

    /**
     * Returns reserved word replacement for symbol provided. 
     * If symbol provided is not on the reserved words list, then throws IllegalStateException.
     * @param symbol
     * @return reserved word for symbol
     */
    public String getReservedWordFor(String symbol) {
        Preconditions.checkNotNull(symbol, "symbol cannot be null");
        String param = symbol.toUpperCase();
        String reservedWord = getPropertyFor(reservedWordsProperties, param);

        return (StringUtils.isEmpty(reservedWord))?symbol:getPropertyFor(reservedWordsProperties, param);
    }

    private String getPropertyFor(Properties properties, String symbol){
        Preconditions.checkNotNull(properties, "properties cannot be null.");
        Preconditions.checkNotNull(symbol, "symbol cannot be null.");
        if(properties.stringPropertyNames().contains(symbol)){
            return properties.getProperty(symbol);
        }else{
            return "";
        }
    }
}
