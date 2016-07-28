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
package eu.ddmore.converters.nonmem.statements.estimation;

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
