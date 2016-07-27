/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
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
