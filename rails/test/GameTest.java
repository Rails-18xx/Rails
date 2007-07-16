package rails.test;

import rails.ui.swing.*;
import rails.util.Config;
import rails.util.Util;

public class GameTest
{
	/** The default properties file name */
	private static String DEFAULT_CONFIG_FILE = "my.properties";
	
   public static void main(String[] args)
   {
	   /* Check if the property file has been set on the command line.
	    * The way to do this is adding an option to the java command:
	    * -Dconfigfile=<property-filename>
	    */
	   String myConfigFile = System.getProperty("configfile");
	   System.out.println("Cmdline configfile setting = "+myConfigFile);
	   
	   /* If not, use the default configuration file name */
	   if (!Util.hasValue(myConfigFile)) {
		   myConfigFile = DEFAULT_CONFIG_FILE;
	   }
	   
	   /* Set the system property that tells log4j to use this file. 
	    * (Note: this MUST be done before updating Config) */ 
	   System.setProperty("log4j.configuration", myConfigFile);
	   /* Tell the properties loader to read this file. */
	   Config.setConfigFile(myConfigFile);
	   System.out.println("Configuration file = "+myConfigFile);
	   
	   /* Start the rails.game selector, which will do all the rest. */
       new GameUIManager();
   }
}
