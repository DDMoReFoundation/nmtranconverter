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
