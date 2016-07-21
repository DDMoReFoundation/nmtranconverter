/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.error;

/**
 * The error statement class which stores error statement and flag to indicate structural observational error. 
 */
public class ErrorStatement {

    private final boolean isStructuralObsError;
    private StringBuilder errorStatement;

    public ErrorStatement(boolean isStructuralObsError) {
        this.isStructuralObsError = isStructuralObsError;
    }

    public StringBuilder getErrorStatement() {
        return errorStatement;
    }

    public void setErrorStatement(StringBuilder errorStatement) {
        this.errorStatement = errorStatement;
    }

    public boolean isStructuralObsError() {
        return isStructuralObsError;
    }
}
