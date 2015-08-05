/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

public enum ErrorType {
    COMBINED_ERROR_1("combinedError1"),
    COMBINED_ERROR_2("combinedError2"),
    COMBINED_ERROR_3("combinedError3"),
    COMBINED_ERROR_2_LOG("combinedError2log"),
    ADDITIVE_ERROR("additiveError"),
    PROPORTIONAL_ERROR("proportionalError");

    private String error;

    ErrorType(String error){
        this.error = error;	
    }

    public String getErrorType() {
        return this.error;
    }
}
