package ddmore.converters.nonmem.statements;

import java.io.PrintWriter;

import crx.converter.engine.ScriptDefinition;

/**
 * Creates and adds estimation statement to nonmem file from script definition.
 * 
 * @author sdeshmukh
 *
 */
public class ThetaStatement extends Parameter {
	
	private ScriptDefinition scriptDefinition;

	public ThetaStatement(String symbId){
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
