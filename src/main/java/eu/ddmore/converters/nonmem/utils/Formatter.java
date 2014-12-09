package eu.ddmore.converters.nonmem.utils;

public class Formatter {
	
	public enum Constant{
		LOG, LOGIT, CORRELATION, FIX, SD;
	}
	
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
