/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import crx.converter.engine.ScriptDefinition;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 *
 */
public class OmegaStatement extends Parameter {

    private ScriptDefinition scriptDefinition;

    public OmegaStatement(String symbId){
        super(symbId);
    }

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public void setScriptDefinition(ScriptDefinition scriptDefinition) {
        this.scriptDefinition = scriptDefinition;
    }

}
