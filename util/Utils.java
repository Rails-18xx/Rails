/*
 * Created on 05-Mar-2005
 *
 *IG Adams
 */
package util;

/**
 * @author Erik Vos
 */
public final class Utils {

    /**
     * No-args private constructor, to prevent (meaningless) construction of one of these. 
     */
    private Utils() {}

    public static boolean hasValue (String s) {
    	return s != null && !s.equals("");
    } 
    
	public static String appendWithComma (String s1, String s2) {
	    StringBuffer b = new StringBuffer (s1 != null ? s1 : "");
	    if (b.length() > 0) b.append (", ");
	    b.append (s1);
	    return b.toString();
	}
}
