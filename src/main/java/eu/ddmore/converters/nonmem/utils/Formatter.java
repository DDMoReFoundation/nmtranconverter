/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

public class Formatter {
	
	public enum Param{
		THETA, OMEGA, SIGMA, BLOCK
	}
	
	public enum Constant{
		LOG, LOGIT, CORRELATION, FIX, SD, T;
	}
	
	public enum ColumnConstant{
		ID, TIME;
	}
	
	public enum TableConstant{
		TABLE, WRES, RES, PRED, NOPRINT, DV, NOAPPEND;
	}
	
	public static final String COMMENT_CHAR = ";";
	private static final String PREFIX = "";//"NM_";
	private static final String NEW_LINE = System.getProperty("line.separator");
	
	public static String addPrefix(String paramName){
		return (!paramName.contains(PREFIX))?PREFIX + paramName.toUpperCase():paramName.toUpperCase();
	}
	
    public static String indent(String text) {
        return "\t" + text;
    }

    public static String endline(String text) {
        return text + endline();
    }
    
    public static String endline() {
        return NEW_LINE;
    }

}
