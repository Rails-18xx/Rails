package rails.util;

import java.util.*;
//import org.apache.commons.logging.*;
import java.io.*;

import org.apache.log4j.Logger;

/**
 * This is a simple utility class with a collection of static functions to load a property object from a property file, to retrieve a particular value from the property file etc.
 * @author Ramiah Bala, rewritten by Erik Vos
 * @version 1.0 
 */
public final class Config {

	//private static Log logger = LogFactory.getLog (Properties.class.getName()); 
	
	/** Default property file name. */
	/* It will be reset from GameTest. */
	private static String myConfigFile = "my.properties";
	
	/** One Properties object for all properties */
	private static Properties prop = new Properties();
	private static boolean loaded = false;
	
	protected static Logger log = Logger.getLogger(Config.class.getPackage().getName());

	/**
	 * Hidden contructor, the class is never instantiated.
	 */
	private Config() {
	}
	
	public static void setConfigFile (String myConfigFile) {
		Config.myConfigFile = myConfigFile;
	}

	public static String get (String key) {
		
		if (prop.isEmpty() || !loaded) {
			/* List the property files to read here */
			load(myConfigFile, false);
			loaded = true;
		}
		if (prop.containsKey(key)) return prop.getProperty(key).trim();
		
		return "";		
	}
	
	/**
	 * This method loads a property file.
	 * @param filename - file key name as a String.
	 * @param required - if TRUE, an exception will be logged 
	 * if the file does not exist.
	 */
	private static void load(String filename, boolean required) {
			
		try {
			log.info ("Loading properties from file "
					+ filename);
			prop.load(
					Config.class.getClassLoader().getResourceAsStream(filename));
			
		} catch (FileNotFoundException FNFE) {
			if (required) {
				System.err.println("Exception whilst loading properties file "+filename);
				FNFE.printStackTrace(System.err);
			}

		} catch (IOException IOE) {
			System.err.println("Exception whilst loading properties file "+filename);
			IOE.printStackTrace(System.err);
		}
	}
}
