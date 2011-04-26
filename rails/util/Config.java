/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Config.java,v 1.13 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.util;

import java.io.File;
import java.io.FileInputStream;
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

import org.apache.log4j.Logger;

import rails.game.ConfigurationException;
import rails.game.GameManager;

/**
 * This is a simple utility class with a collection of static functions to load
 * a property object from a property file, to retrieve a particular value from
 * the property file etc.
 *
 * @author Ramiah Bala, rewritten by Erik Vos, rewritten by Stefan Frey
 * @version 2.0
 */
public final class Config {

    protected static Logger log;

    /** Commandline options */
    private static final String CONFIGFILE_CMDLINE = "configfile";
    private static final String PROFILE_CMDLINE = "profile";

    /** XML setup */
    private static final String CONFIG_XML_DIR = "data";
    private static final String CONFIG_XML_FILE = "Properties.xml";
    private static final String CONFIG_TAG = "Properties";
    private static final String SECTION_TAG = "Section";
    private static final String ITEM_TAG = "Property";

    /** Log 4j configuration */
    private static final String LOG4J_CONFIG_FILE = "log4j.properties";
        
    /** Rails profile configurations */
    private static String defaultProfilesFile = "data/profiles/default.profiles";
    private static Properties defaultProfiles = new Properties();
    private static String userProfilesFile = "user.profiles";
    private static Properties userProfiles = new Properties();
    private static boolean profilesLoaded = false;
    private static String DEFAULT_PROFILE_SELECTION = "default"; // can be overwritten
    private static final String TEST_PROFILE_SELECTION = ".test"; // used as default profile for integration tests
    private static final String STANDARD_PROFILE_SELECTION = "user";
    private static final String DEFAULTPROFILE_PROPERTY = "default.profile";
    private static final String PROFILENAME_PROPERTY = "profile.name";
    
    /** selected profile */
    private static String selectedProfile;
    private static boolean legacyConfigFile;
    
    /** properties storage. */
    private static Properties defaultProperties = new Properties();
    private static Properties userProperties = new Properties();
    private static boolean propertiesLoaded = false;
    
    /** Map that holds the panel, which contains config items */
    private static Map<String, List<ConfigItem>> configSections = null; 
    
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
            // Find the config tag inside the the config xml file
            Tag configTag =
                    Tag.findTopTagInFile(CONFIG_XML_FILE, directories, CONFIG_TAG);
            log.debug("Opened config xml, filename = " + CONFIG_XML_FILE);
            
            // define sections
            configSections = new LinkedHashMap<String, List<ConfigItem>>();

            // find sections
            List<Tag> sectionTags = configTag.getChildren(SECTION_TAG);
            if (sectionTags != null) {
                for (Tag sectionTag:sectionTags) {
                    // find name attribute
                    String sectionName = sectionTag.getAttributeAsString("name");
                    if (!Util.hasValue(sectionName)) continue;
                    
                    // find items
                    List<Tag> itemTags = sectionTag.getChildren(ITEM_TAG);
                    if (itemTags == null || itemTags.size() == 0) continue;
                    List<ConfigItem> sectionItems = new ArrayList<ConfigItem>();
                    for (Tag itemTag:itemTags) {
                        sectionItems.add(new ConfigItem(itemTag));
                    }
                    configSections.put(sectionName, sectionItems);
                }
            }
            
        } catch (ConfigurationException e) {
            log.error("Configuration error in setup of " + CONFIG_XML_FILE + ", exception = " + e);
        }
    }
    
    public static Map<String, List<ConfigItem>> getConfigSections() {
        if (configSections == null) {
            readConfigSetupXML();
        }
        log.debug("Configuration setup = " + configSections);
        return configSections;
    }
    
    public static int getMaxElementsInPanels() {
        int maxElements = 0;
        for (List<ConfigItem> panel:configSections.values()) {
            maxElements = Math.max(maxElements, panel.size());
        }
        log.debug("Configuration sections with maximum elements of " + maxElements);
        return maxElements;
    }
    
    /**
     * updates the profile according to the changes in configitems
     */
    public static void updateProfile(boolean applyInitMethods) {
        for (List<ConfigItem> items:configSections.values()) {
            for (ConfigItem item:items) {
                if (!item.hasNewValue()) continue;
                if (item.getNewValue().equals(defaultProperties.get(item.name))) {
                    userProperties.remove(item.name);
                    continue;
                }
                userProperties.setProperty(item.name, item.getNewValue());
                if (applyInitMethods) item.callInitMethod();
                log.debug("Changed property name = " + item.name + " to value = " + item.getNewValue());
                item.setNewValue(null);
            }
        }
    }
    
    /**
     * reverts all changes in configitems
     */
    public static void revertProfile() {
        for (List<ConfigItem> items:configSections.values()) {
            for (ConfigItem item:items) {
                item.setNewValue(null);
            }
        }
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
     * save active Profile
     */
    public static boolean saveActiveProfile() {
        String filepath = userProfiles.getProperty(selectedProfile);
        if (Util.hasValue(filepath)) {
            return storePropertyFile(userProperties, filepath);
        } else {
            return false;
        }
    }
    
    /**
     * change active Profile 
     */
    public static boolean changeActiveProfile(String profileName) {
        readConfigSetupXML();
        loadProfile(profileName);
        selectedProfile = profileName;
        return true;
    }
    
    /**
     * create new profile
     */
    public static boolean createUserProfile(String profileName, String defaultProfile) {
        userProperties = new Properties();
        defaultProperties = new Properties();
        
        // add to list of user profiles
        userProfiles.setProperty(profileName, "");
        
        // define and load default profile
        String defaultConfigFile = defaultProfiles.getProperty(defaultProfile);
        userProperties.setProperty(PROFILENAME_PROPERTY, profileName);
        userProperties.setProperty(DEFAULTPROFILE_PROPERTY, defaultProfile);
        loadPropertyFile(defaultProperties, defaultConfigFile, true);
        setSaveDirDefaults();

        selectedProfile = profileName;
        return true;
    }
    
    
    private static Map<String, String> convertProperties(Properties properties, boolean visibleOnly) {
        Map<String, String> converted = new HashMap<String, String>();
        for (Object key:properties.keySet()) {
            if (visibleOnly && ((String)key).substring(0,1).equals(".")) continue;
            converted.put((String) key, (String) properties.get(key));
        }
        return converted;
    }
    
    /** 
     * get all default profiles 
     */
    public static List<String> getDefaultProfiles(boolean visibleOnly) {
        List<String> profiles = new ArrayList<String>(convertProperties(defaultProfiles, visibleOnly).keySet());
        Collections.sort(profiles);
        return profiles;
    }
    
    public static String getDefaultProfileSelection() {
        return DEFAULT_PROFILE_SELECTION;
    }

    /** 
     * get all user profiles 
     */
    public static List<String> getUserProfiles() {
        List<String> profiles = new ArrayList<String>(convertProperties(userProfiles, true).keySet());
        Collections.sort(profiles);
        return profiles;
    }
    
    /**
     * get all (visible default + user) profiles
     */
    public static List<String> getAllProfiles() {
        List<String> profiles = getDefaultProfiles(true);
        profiles.addAll(getUserProfiles());
        return profiles;
    }
    
    /**
     * checks if profile is default profile
     */
    public static boolean isDefaultProfile(String profileName) {
        return !(defaultProfiles.get(profileName) == null);
    }
        
    /**
     * returns name of (active) default profile
     */
    public static String getDefaultProfileName() {
        return userProperties.getProperty(DEFAULTPROFILE_PROPERTY);
    }
    
    /**
     * returns name of active profile
     */
    public static String getActiveProfileName() {
        return selectedProfile;
    }
    
    /**
     * returns true if legacy configfile is used
     */
    public static boolean isLegacyConfigFile() {
        return legacyConfigFile;
    }
    
    /**
     * sets filename for an active profile (and store list of profiles)
     * @return false if list of profiles cannot be stored
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
     * @return if user location is defined
     */
    public static boolean isFilePathDefined() {
        return Util.hasValue(userProfiles.getProperty(selectedProfile));
    }

    
    /**
     * activates settings used for testing
     */
    public static void setConfigTest() {
        /*
         * Set the system property that tells log4j to use this file. (Note:
         * this MUST be done before updating Config)
         */
        String log4jSelection = System.getProperty("log4j.configuration");
        if (!Util.hasValue(log4jSelection)) {
            log4jSelection = LOG4J_CONFIG_FILE;
        }
        System.setProperty("log4j.configuration", log4jSelection);
        System.out.println("log4j.configuration =  " + log4jSelection);

        // delayed setting of logger
        log = Logger.getLogger(Config.class.getPackage().getName());

        // define settings for testing 
        legacyConfigFile = false;
        DEFAULT_PROFILE_SELECTION = TEST_PROFILE_SELECTION;
        selectedProfile = null;

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
        String log4jSelection = System.getProperty("log4j.configuration");
        if (!Util.hasValue(log4jSelection)) {
            log4jSelection = LOG4J_CONFIG_FILE;
        }
        System.setProperty("log4j.configuration", log4jSelection);
        System.out.println("log4j.configuration =  " + log4jSelection);

        // delayed setting of logger
        log = Logger.getLogger(Config.class.getPackage().getName());
        
        /*
         * Check if the profile has been set from the command line
         * to do this is adding an option to the java command: -Dprofile=<profile-name>
         */
        String configSelection = System.getProperty(PROFILE_CMDLINE);
        System.out.println("Cmdline profile selection = " + configSelection);

        legacyConfigFile = false;
        if (configSelection == null) {
            /*
             * Check if the property file has been set on the command line. The way
             * to do this is adding an option to the java command: -Dconfigfile=<property-filename>
             * 
             * This is for legacy reasons only
             */
            configSelection = System.getProperty(CONFIGFILE_CMDLINE);
            
            if (Util.hasValue(configSelection)) {
                System.out.println("Cmdline configfile selection (legacy!) = " + configSelection);
                legacyConfigFile = true;
            }
        }
        
        /* if nothing has selected so far, choose standardProfile */
        if (!Util.hasValue(configSelection)) {
            configSelection = STANDARD_PROFILE_SELECTION;
        }
        
        selectedProfile = configSelection;
        if (!legacyConfigFile) {
            System.out.println("Profile selection = " + selectedProfile);
        }
        
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
            loadProfile(selectedProfile);
            propertiesLoaded  = true;
        }
    }
    
    
    /**
     * loads an external user profile
     * defined by the filepath
     */
    public static boolean loadProfileFromFile(File file) {
        String filepath = file.getPath();
        if (loadPropertyFile(userProperties, filepath, false)) {
            String profile = userProperties.getProperty(PROFILENAME_PROPERTY);
            if (!Util.hasValue(profile)) {
                profile = STANDARD_PROFILE_SELECTION;
            }
            selectedProfile = profile;
            setActiveFilepath(filepath); // do not set filepath on import
            loadDefaultProfile();
            setSaveDirDefaults();
            return true;
        } else {
            return false;
        }
    }

    /**
     * imports an external user profile into an existing profile
     * defined by the filepath
     */
    public static boolean importProfileFromFile(File file) {
        String filepath = file.getPath();
        Properties importProperties = new Properties();
        if (loadPropertyFile(importProperties, filepath, false)) {
            userProperties.putAll(importProperties);
            setSaveDirDefaults();
            return true;
        } else {
            return false;
        }
    }
    
    
    /**
     * loads a user profile
     * if not defined or loadable, creates a default user profile
     */
    private static void loadProfile(String userProfile) {
        // reset properties
        userProperties = new Properties();
        defaultProperties = new Properties();
        
        String userConfigFile = null;
        if (Util.hasValue(userProfile)) {
            // check if the profile is already defined under userProfiles 
            userConfigFile = userProfiles.getProperty(userProfile);
            if (Util.hasValue(userConfigFile) && // load user profile
                    loadPropertyFile(userProperties, userConfigFile, false)) {
                // do nothing, only side effects
            } else { // if not defined or loadable, define userprofile with file association
                userProfiles.setProperty(userProfile, "");
            }

            // check if profilename is defined in user properties
            if (!Util.hasValue(userProfiles.getProperty(PROFILENAME_PROPERTY))) {
                userProperties.setProperty(PROFILENAME_PROPERTY, userProfile);
            }
        }

        loadDefaultProfile();
        setSaveDirDefaults();
    }
    
    /**
     *  loads the associated default profile
     *  if none is defined, uses standard default profile
     */
    private static void loadDefaultProfile() {
        String defaultConfigFile = null;
        String defaultConfig = userProperties.getProperty(DEFAULTPROFILE_PROPERTY);
        if (defaultConfig != null) {
            defaultConfigFile = defaultProfiles.getProperty(defaultConfig);
        }
        if (defaultConfigFile == null) {
            defaultConfigFile = defaultProfiles.getProperty(DEFAULT_PROFILE_SELECTION);
            userProperties.setProperty(DEFAULTPROFILE_PROPERTY, DEFAULT_PROFILE_SELECTION);
        }
        loadPropertyFile(defaultProperties, defaultConfigFile, true);
    }

    /**
     * This method loads a property file.
     *
     * @param properties - the property to store
     * @param filepath - filename as a String.
     * @param resource - if TRUE, loaded from jar (via classloader), otherwise from filesystem
     * @return TRUE if load was successful
     */
    private static boolean loadPropertyFile(Properties properties, String filepath, boolean resource) {
        
        boolean result = true; 
        try {
            log.info("Loading properties from file " + filepath);
            InputStream inFile;
            if (resource) {
                inFile = Config.class.getClassLoader().getResourceAsStream(filepath);  
            } else {
                inFile = new FileInputStream(filepath);
            }
            properties.load(inFile);
        } catch (Exception e) {
            log.error(e + " whilst loading properties file "
                               + filepath, e);
            result = false;
        }
        return result;
    }

    /**
     * This method stores a property file
     * @param properties - 
     * @param filepath
     * @return
     */
    
    private static boolean storePropertyFile(Properties properties, String filepath) {
        File outFile = new File(filepath);
        boolean result = true;
        try { 
            properties.store(new FileOutputStream(outFile), "Automatically generated, do not edit");
            log.info("Storing properties to file " + filepath);
        } catch (IOException e) {
            log.error(e + " whilst storing properties file "
                    + filepath);
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
