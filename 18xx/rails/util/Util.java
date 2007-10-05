/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Util.java,v 1.7 2007/10/05 22:02:26 evos Exp $*/
package rails.util;

import rails.game.ConfigurationException;

public final class Util
{
	/**
	 * No-args private constructor, to prevent (meaningless) construction of one
	 * of these.
	 */
	private Util()
	{
	}

	public static boolean hasValue(String s)
	{
		return s != null && !s.equals("");
	}

	public static String appendWithDelimiter(String s1, String s2, String delimiter)
	{
		StringBuffer b = new StringBuffer(s1 != null ? s1 : "");
		if (b.length() > 0)
			b.append(delimiter);
		b.append(s2);
		return b.toString();
	}

	/** Check if an object is an instance of a class - at runtime! */
	public static boolean isInstanceOf(Object o, Class clazz)
	{
		Class c = o.getClass();
		while (c != null)
		{
			if (c == clazz)
				return true;
			c = c.getSuperclass();
		}
		return false;
	}
	
	public static String getClassShortName (Object object) {
	    return object.getClass().getName().replaceAll(".*\\.", "");
	}

    public static int parseInt (String value) 
    throws ConfigurationException {
        
        if (!hasValue(value)) return 0;
        
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException ("Invalid integer value: "+value, e);
        }
    }
}
