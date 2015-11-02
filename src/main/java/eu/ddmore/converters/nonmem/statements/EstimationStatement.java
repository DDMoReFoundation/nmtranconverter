/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 *
 */
public class EstimationStatement {

    private final EstimationDetailsEmitter estimationEmitter;

    private final String simStart = Formatter.endline(";Sim_start");
    private final String simContentEnd = Formatter.endline(";$SIM (12345) (12345 UNIFORM) ONLYSIM NOPREDICTION")
            +Formatter.endline(";Sim_end");

    public EstimationStatement(EstimationDetailsEmitter estimationEmitter){
        Preconditions.checkNotNull(estimationEmitter, "estimation statement handler cannot be null");
        this.estimationEmitter = estimationEmitter;
    }

    /**
     * Get estimation related statements and associated details as output.  
     * @return all the associated statements
     */
    public String getStatementsWithEstimationDetails(){
        StringBuilder statement = new StringBuilder();

        if(!estimationEmitter.getEstimationStatement().toString().isEmpty()){
            statement.append(Formatter.endline());
            statement.append(estimationEmitter.addSimContentForDiscrete(simStart));

            statement.append(estimationEmitter.getEstimationStatement());
            statement.append(estimationEmitter.getCovStatement());

            statement.append(estimationEmitter.addSimContentForDiscrete(simContentEnd));
        }
        return statement.toString();
    }
}
