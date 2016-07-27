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
package eu.ddmore.converters.nonmem.statements;

import crx.converter.spi.steps.SimulationStep;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class SimulationStatement {

    private SimulationStep simulationStep;


    public SimulationStep getSimulationStep() {
        return simulationStep;
    }

    public void setSimulationStep(SimulationStep simulationStep) {
        this.simulationStep = simulationStep;
    }

    public SimulationStatement(SimulationStep simulationStep){
        this.simulationStep = simulationStep;
    }

    public String getSimulationStatement(){
        StringBuilder stringBuilder = new StringBuilder();

        if(getSimulationStep()!=null){
            stringBuilder.append(Formatter.endline());
            stringBuilder.append(Formatter.sim());
            //234251 is just random number
            stringBuilder.append("(234251)");
        }
        return stringBuilder.toString();

    }
}
