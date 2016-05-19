/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
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
