/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
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
        for(String variable : varEquations.keySet()){
            variableEquations.append(Formatter.endline(variable+"="+varEquations.get(variable)));
        }
        super.setErrorStatement(variableEquations);
    }

    public Map<String, String> getVarEquations() {
        return varEquations;
    }
}
