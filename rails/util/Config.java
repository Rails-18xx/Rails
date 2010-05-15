/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Config.java,v 1.12 2010/05/15 22:44:46 evos Exp $*/
package rails.util;

import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This is a simple utility class with a collection of static functions to load
 * a property object from a property file, to retrieve a particular value from
 * the property file etc.
 *
 * @author Ramiah Bala, rewritten by Erik Vos
 * @version 1.0
 */
public final class Config {

    /** Default property file name. */
    /* It will be reset from GameTest. */
    private static String myConfigFile = "my.properties";
    //private static String gamesConfigFile = "games.properties";

    /** One Properties object for all properties */
    private static Properties prop = new Properties();
    private static boolean loaded = false;

    protected static Logger log =
            Logger.getLogger(Config.class.getPackage().getName());

    /**
     * Hidden contructor, the class is never instantiated.
     */
    private Config() {}

    public static void setConfigFile(String myConfigFile) {
        Config.myConfigFile = myConfigFile;
        load();
    }

    public static String get(String key) {

        if (prop.isEmpty() || !loaded) {
            load();
        }
        if (prop.containsKey(key)) return prop.getProperty(key).trim();

        return "";
    }
    
    public static String get(String key, String defaultValue) {

        if (prop.isEmpty() || !loaded) {
            load();
        }
        if (prop.containsKey(key)) return prop.getProperty(key).trim();

        return defaultValue;
    }
    
    private static void load() {
        
        if (prop.isEmpty() || !loaded) {
            /* List the property files to read here */
            load(myConfigFile, false);
            //load(gamesConfigFile, false);
            setDefaults();
            loaded = true;
        }
    }

    /**
     * This method loads a property file.
     *
     * @param filename - file key name as a String.
     * @param required - if TRUE, an exception will be logged if the file does
     * not exist.
     */
    private static void load(String filename, boolean required) {

        try {
            log.info("Loading properties from file " + filename);
            prop.load(Config.class.getClassLoader().getResourceAsStream(
                    filename));

        } catch (FileNotFoundException FNFE) {
            if (required) {
                System.err.println("File not found: " + filename);
            }

        } catch (Exception e) {
            System.err.println(e + " whilst loading properties file "
                               + filename);
            e.printStackTrace(System.err);
        }
    }
    
    private static void setDefaults() {
        if (!Util.hasValue(prop.getProperty("save.directory"))) {
            log.debug("Setting save directory to "+System.getProperty("user.dir"));
            prop.put("save.directory", System.getProperty("user.dir"));
        }
    }
}
