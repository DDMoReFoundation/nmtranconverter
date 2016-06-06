/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import crx.converter.engine.ScriptDefinition;

/**
 * Represents omega parameter in an nmtran model.
 */
public class OmegaParameter extends Parameter {

    private ScriptDefinition scriptDefinition;

    public OmegaParameter(String symbId){
        super(symbId);
    }

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public void setScriptDefinition(ScriptDefinition scriptDefinition) {
        this.scriptDefinition = scriptDefinition;
    }

}
