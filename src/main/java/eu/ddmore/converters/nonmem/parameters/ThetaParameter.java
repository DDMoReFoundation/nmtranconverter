/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.io.PrintWriter;

import crx.converter.engine.ScriptDefinition;

/**
 * This class represents theta parameter in an nmtran model.
 */
public class ThetaParameter extends Parameter {

    private ScriptDefinition scriptDefinition;

    public ThetaParameter(String symbId){
        super(symbId);
    }

    public void getThetaStatement(PrintWriter fout){

    }

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public void setScriptDefinition(ScriptDefinition scriptDefinition) {
        this.scriptDefinition = scriptDefinition;
    }

}
