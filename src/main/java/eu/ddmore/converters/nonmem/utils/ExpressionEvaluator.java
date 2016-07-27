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
package eu.ddmore.converters.nonmem.utils;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;


/**
 * Evaluates mathematical formulas specified by string.
 */
public class ExpressionEvaluator {
    private static final Logger LOG = Logger.getLogger(ExpressionEvaluator.class);
    public static Double evaluate(String expr) {
        LOG.debug(String.format("Expression is %s", expr));
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try {
            Double result = (Double)engine.eval(expr);
            LOG.debug(String.format("Result is %s", result));
            return result;
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format("Could not evaluate expression %s", expr), e);
        }
    }
}
