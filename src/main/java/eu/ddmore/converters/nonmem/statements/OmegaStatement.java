package eu.ddmore.converters.nonmem.statements;

import crx.converter.engine.ScriptDefinition;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
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
