/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Config.java,v 1.13 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import rails.game.ConfigurationException;
import rails.game.GameManager;

/**
 * This is a simple utility class with a collection of static functions to load
 * a property object from a property file, to retrieve a particular value from
 * the property file etc.
 *
 * @author Ramiah Bala, rewritten by Erik Vos
 * @version 1.0
 */
public final class Config {

    protected static Logger log =
        Logger.getLogger(Config.class.getPackage().getName());


    /** XML setup */
    private static final String CONFIG_XML_DIR = "data";
    private static final String CONFIG_XML_FILE = "Properties.xml";
    private static final String CONFIG_TAG = "Properties";
    private static final String PANEL_TAG = "Panel";
    private static final String ITEM_TAG = "Property";

    /** Log 4j configuration */
    private static final String LOG4J_CONFIG_FILE = "log4j.properties";
    
    
    /** Rails profile configurations */
    private static String defaultProfilesFile = "default.profiles";
    private static Properties defaultProfiles = new Properties();
    private static String userProfilesFile = "user.profiles";
    private static Properties userProfiles = new Properties();
    private static boolean profilesLoaded = false;
    private static final String TEST_PROFILE_SELECTION = "test";
    private static final String DEFAULT_PROFILE_SELECTION = "default";
    private static final String DEFAULT_PROFILE_PROPERTY = "default.profile";
    private static final String STANDARD_PROFILE_SELECTION = "user";
    
    /** selected profile */
    private static String selectedProfile;
    private static boolean legacyConfigFile;
    
    /** properties storage. */
    private static Properties defaultProperties = new Properties();
    private static Properties userProperties = new Properties();
    private static boolean propertiesLoaded = false;
    
    /** Map that holds the panel, which contains config items */
    private static Map<String, List<ConfigItem>> configPanels = null; 
    
    /**
     * Hidden constructor, the class is never instantiated, everything is static
     */
    private Config() {}

    /** 
     * Reads the config.xml file that defines all config items
     */
    public static void readConfigSetupXML() {
        List<String> directories = new ArrayList<String>();
        directories.add(CONFIG_XML_DIR);
        try {
            // Find the <Config> tag
            Tag configTag =
                    Tag.findTopTagInFile(CONFIG_XML_FILE, directories, CONFIG_TAG);
            log.debug("Opened config xml, filename = " + CONFIG_XML_FILE);
            
            configPanels = new LinkedHashMap<String, List<ConfigItem>>();
            // find panels
            List<Tag> panelTags = configTag.getChildren(PANEL_TAG);
            if (panelTags != null) {
                for (Tag panelTag:panelTags) {
                    // find name attribute
                    String panelName = panelTag.getAttributeAsString("name");
                    if (!Util.hasValue(panelName)) continue;
                    
                    // find items
                    List<Tag> itemTags = panelTag.getChildren(ITEM_TAG);
                    if (itemTags == null || itemTags.size() == 0) continue;
                    List<ConfigItem> panelItems = new ArrayList<ConfigItem>();
                    for (Tag itemTag:itemTags) {
                        panelItems.add(new ConfigItem(itemTag));
                    }
                    configPanels.put(panelName, panelItems);
                }
            }
            
        } catch (ConfigurationException e) {
            log.error("Configuration error in setup of ");
        }
    }
    
    public static Map<String, List<ConfigItem>> getConfigPanels() {
        if (configPanels == null) {
            readConfigSetupXML();
        }
        log.debug("Configuration setup = " + configPanels);
        return configPanels;
    }
    
    /**
     * First tries to return {key}.{gameName}, if undefined returns {key} 
     */
    public static String getGameSpecific(String key) {
        return Config.getSpecific(key, GameManager.getInstance().getGameName());
    }
    
    /**
     * First tries to return {key}.{appendix}, if undefined returns {key}
     */
    public static String getSpecific(String key, String appendix) {
        String value = Config.get(key + "." + appendix);
        if (value == "") {
            value = Config.get(key);
        }
        return value;
    }
    
    public static String get(String key) {
        return get(key, "");
    }
    
    public static String get(String key, String defaultValue) {
        if (defaultProperties.isEmpty() || !propertiesLoaded) {
            initialLoad();
        }
        if (userProperties.containsKey(key)) return userProperties.getProperty(key).trim();
        if (defaultProperties.containsKey(key)) return defaultProperties.getProperty(key).trim();

        return defaultValue;
    }
    

    /**
     * @return if user location is defined
     */
    public static boolean isFilePathDefined() {
        return Util.hasValue(userProfiles.getProperty(selectedProfile));
    }
    
    private static boolean storePropertyFile(Properties properties, String filepath) {
        File outFile = new File(filepath);
        boolean result = true;
        try { 
            properties.store(new FileOutputStream(outFile), "Automatically generated, do not edit");
        } catch (IOException e) {
            result = false;
        }
        return result;
    }
    
    /**
     * save active Profile
     */
    public static boolean saveActiveProfile(String filepath) { 
        return storePropertyFile(userProperties, filepath);
    }
    
    /**
     * change active Profile 
     */
    public static boolean changeActiveProfile(String profileName) {
        readConfigSetupXML();
        loadPropertyProfile(profileName);
        selectedProfile = profileName;
        return true;
    }
    
    private static Map<String, String> convertProperties(Properties properties) {
        Map<String, String> converted = new HashMap<String, String>();
        for (Object key:properties.keySet()) {
            converted.put((String) key, (String) properties.get(key));
        }
        return converted;
    }
    
    
    /** 
     * get all default profiles 
     */
    public static List<String> getDefaultProfiles() {
        List<String> profiles = new ArrayList<String>(convertProperties(defaultProfiles).keySet());
        profiles.remove(DEFAULT_PROFILE_PROPERTY);
        Collections.sort(profiles);
        return profiles;
    }

    /** 
     * get all user profiles 
     */
    public static List<String> getUserProfiles() {
        List<String> profiles = new ArrayList<String>(convertProperties(userProfiles).keySet());
        Collections.sort(profiles);
        return profiles;
    }
    
    /**
     * returns name of (active) default profile
     */
    public static String getDefaultProfileName() {
        return userProperties.getProperty(DEFAULT_PROFILE_PROPERTY);
    }
    
    /**
     * returns name of active profile
     */
    public static String getActiveProfileName() {
        return selectedProfile;
    }
    
    /**
     * sets filename for an active profile (and store list of profiles)
     */
    public static boolean setActiveFilepath(String filepath) {
        userProfiles.setProperty(selectedProfile, filepath);
        return storePropertyFile(userProfiles, userProfilesFile);
    }
    
    /**
     * returns filename of active profile, (null if undefined or default profile)
     */
    public static String getActiveFilepath() {
        return userProfiles.getProperty(selectedProfile);
    }
    
    /**
     * activates settings used for testing
     */
    public static void setConfigTest() {
        /*
         * Set the system property that tells log4j to use this file. (Note:
         * this MUST be done before updating Config)
         */
        System.setProperty("log4j.configuration", LOG4J_CONFIG_FILE);
        legacyConfigFile = false;
        selectedProfile = TEST_PROFILE_SELECTION;
        initialLoad();
    }

    
    /**
     * activates configuration settings based on default settings
     */
    public static void setConfigSelection() {
        /*
         * Set the system property that tells log4j to use this file. (Note:
         * this MUST be done before updating Config)
         */
        System.setProperty("log4j.configuration", LOG4J_CONFIG_FILE);
        
        /*
         * Check if the profile has been set from the command line
         * to do this is adding an option to the java command: -Dprofile=<profile-name>
         */
        String configSelection = System.getProperty("profile");
        System.out.println("Cmdline profile selection = " + configSelection);

        legacyConfigFile = false;
        if (configSelection == null) {
            /*
             * Check if the property file has been set on the command line. The way
             * to do this is adding an option to the java command: -Dconfigfile=<property-filename>
             * 
             * This is for legacy reasons only
             */
            configSelection = System.getProperty("configfile");
            
            if (configSelection != null) {
                System.out.println("Cmdline configfile selection (legacy!) = " + configSelection);
                legacyConfigFile = true;
            }
        }
        
        /* if nothing has selected so far, choose standardProfile */
        if (!Util.hasValue(configSelection)) {
            configSelection = STANDARD_PROFILE_SELECTION;
        }
        
        selectedProfile = configSelection;
        initialLoad();
    }

    
    private static void initialLoad() {
        if (legacyConfigFile) {
            if (!propertiesLoaded) {
                loadPropertyFile(defaultProperties, selectedProfile, false);
                propertiesLoaded = true;
                setSaveDirDefaults();
            }
            return;
        }
        
        if (!profilesLoaded) {
            loadPropertyFile(defaultProfiles, defaultProfilesFile, true);
            loadPropertyFile(userProfiles, userProfilesFile, false);
            profilesLoaded = true;
        }
        
        /* Tell the properties loader to read this file. */
        log.info("Selected profile = " + selectedProfile);

        if (!propertiesLoaded) {
            loadPropertyProfile(selectedProfile);
            propertiesLoaded  = true;
        }
    }
    
    /**
     * loads a user profile and the according default profile
     * if not defined or loadable, creates a default user profile
     */
    private static void loadPropertyProfile(String userProfile) {
        // reset properties
        userProperties = new Properties();
        defaultProperties = new Properties();
        
        // check if the profile is already defined under userProfiles 
        String userConfigFile = userProfiles.getProperty(userProfile);
        String defaultConfigFile = null;
        if (Util.hasValue(userConfigFile) && // load user profile
             loadPropertyFile(userProperties, userConfigFile, false)) {
                String defaultConfig = userProperties.getProperty(DEFAULT_PROFILE_PROPERTY);
                if (defaultConfig != null) {
                    defaultConfigFile = defaultProfiles.getProperty(defaultConfig);
                }
        } else {
            userProfiles.setProperty(userProfile, "");
        }
        if (defaultConfigFile == null) {
            defaultConfigFile = defaultProfiles.getProperty(DEFAULT_PROFILE_SELECTION);
            userProperties.setProperty(DEFAULT_PROFILE_PROPERTY, DEFAULT_PROFILE_SELECTION);
        }
        loadPropertyFile(defaultProperties, defaultConfigFile, true);
        setSaveDirDefaults();
    }

    /**
     * This method loads a property file.
     *
     * @param filename - file key name as a String.
     * @param resource - if TRUE, loaded from jar (via classloader), otherwise from filesystem
     * @return TRUE if load was successful
     */
    private static boolean loadPropertyFile(Properties properties, String filename, boolean resource) {
        
        boolean result = true; 
        try {
            log.info("Loading properties from file " + filename);
            InputStream inFile;
            if (resource) {
                inFile = Config.class.getClassLoader().getResourceAsStream(filename);  
            } else {
                inFile = new FileInputStream(filename);
            }
            properties.load(inFile);
        } catch (Exception e) {
            System.err.println(e + " whilst loading properties file "
                               + filename);
            e.printStackTrace(System.err);
            result = false;
        }
        return result;
    }
    
    private static void setSaveDirDefaults() {
        if (!Util.hasValue(defaultProperties.getProperty("save.directory"))) {
            log.debug("Setting save directory to "+System.getProperty("user.dir"));
            defaultProperties.put("save.directory", System.getProperty("user.dir"));
        }
    }
}
