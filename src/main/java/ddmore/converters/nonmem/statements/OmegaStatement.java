package ddmore.converters.nonmem.statements;

import java.io.PrintWriter;

import crx.converter.engine.ScriptDefinition;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class OmegaStatement {
	
	private ScriptDefinition scriptDefinition;

	public OmegaStatement(ScriptDefinition scriptDefinition){
		this.setScriptDefinition(scriptDefinition);
	}
	
	public void getOmegaStatement(PrintWriter fout){
		
	}

	public ScriptDefinition getScriptDefinition() {
		return scriptDefinition;
	}

	public void setScriptDefinition(ScriptDefinition scriptDefinition) {
		this.scriptDefinition = scriptDefinition;
	}
	
}
