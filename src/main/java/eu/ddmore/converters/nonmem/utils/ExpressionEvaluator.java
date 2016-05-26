/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
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
