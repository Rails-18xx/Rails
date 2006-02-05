/*
 * Created on 05-Mar-2005
 *
 *IG Adams
 */
package util;

/**
 * @author Erik Vos
 */
public final class Util {

    /**
     * No-args private constructor, to prevent (meaningless) construction of one of these. 
     */
    private Util() {}

    public static boolean hasValue (String s) {
    	return s != null && !s.equals("");
    } 
    
	public static String appendWithComma (String s1, String s2) {
	    StringBuffer b = new StringBuffer (s1 != null ? s1 : "");
	    if (b.length() > 0) b.append (", ");
	    b.append (s2);
	    return b.toString();
	}
	
	/** Check if an object is an instance of a class - at runtime! */
	public static boolean isInstanceOf (Object o, Class clazz) {
	    Class c = o.getClass();
	    while (c != null) {
	        if (c == clazz) return true;
	        c = c.getSuperclass();
	    }
	    return false;
	}
}
