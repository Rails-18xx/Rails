/*
 * Created on 05-Mar-2005
 *
 *IG Adams
 */
package util;

import game.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	/** Open an input stream from a file, which may exist as a physical file
	 * or in a JAR file. The name must be valid for both options.
	 * @author Erik Vos
	 */
	public static InputStream getStreamForFile (String fileName) 
	throws IOException {

	     File file = new File(fileName);
	     if (file.exists()) {
             return new FileInputStream (file);
	     } else {
	          // Search in the jar
	          File jarFile = new File ("./Rails.jar");
              JarFile jf = new JarFile (jarFile);
              JarInputStream jis = new JarInputStream (new FileInputStream(jarFile));
              for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry()) {
                  if (fileName.equals(je.getName())) {
                      return jis;
                  }
              }
              return null;
	      }
	}
}
