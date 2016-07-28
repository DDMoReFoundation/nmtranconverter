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
 * enum with Error types associated with structural obervation error 
 */
public enum StructuralObsErrorType {
    COMBINED_ERROR_1("combinedError1"),
    COMBINED_ERROR_2("combinedError2"),
    COMBINED_ERROR_3("combinedError3"),
    COMBINED_ERROR_2_LOG("combinedError2Log"),
    ADDITIVE_ERROR("additiveError"),
    PROPORTIONAL_ERROR("proportionalError");

    private String error;

    StructuralObsErrorType(String error) {
        this.error = error;	
    }

    public String getErrorType() {
        return this.error;
    }
    
    public static StructuralObsErrorType fromErrorType(final String errorTypeString) {
        for (final StructuralObsErrorType et : values()) {
            if (et.getErrorType().equals(errorTypeString)) {
                return et;
            }
        }
        throw new IllegalArgumentException(String.format("No ErrorType enum constant for error type string \"%s\"", errorTypeString));
    }
}
