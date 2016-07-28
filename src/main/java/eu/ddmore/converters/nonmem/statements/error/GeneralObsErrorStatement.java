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

import java.util.Map;
import com.google.common.base.Preconditions;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Initialises and stores general observation error statement and related information for nmtran
 */
public class GeneralObsErrorStatement extends ErrorStatement {

    private final Map<String, String> varEquations;

    public GeneralObsErrorStatement(Map<String, String> varEquations, boolean isStructuralObsError){
        super(isStructuralObsError);
        Preconditions.checkNotNull(varEquations, "variable equations should not be null.");
        this.varEquations = varEquations;
        populateErrorStatement();
    }

    private void populateErrorStatement(){
        StringBuilder variableEquations = new StringBuilder();
        for(String rhs : varEquations.values()){
            variableEquations.append(Formatter.endline(rhs));
        }
        super.setErrorStatement(variableEquations);
    }

    public Map<String, String> getVarEquations() {
        return varEquations;
    }
}
