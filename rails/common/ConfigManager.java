package rails.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import rails.common.ConfigManager;
import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.Game;
import rails.game.GameManagerI;
import rails.util.SystemOS;
import rails.util.Util;

/**
 * ConfigManager is a utility class that collects all functions
 * used to define and control configuration options
 * 
 * It is a rewrite of the previouslsy used static class Config
 */

public class ConfigManager implements ConfigurableComponentI {

    protected static Logger log;
    
    // STATIC CONSTANTS
    
    // Log4j command line option, compare log4j documentation
    private static final String LOG4J_CLI_OPTION = "log4j.configuration";
    // Default Log4j configuration-file
    private static final String LOG4J_CONFIG_FILE = "log4j.properties";
    
    //  XML setup
    private static final String CONFIG_XML_DIR = "data";
    private static final String CONFIG_XML_FILE = "Properties.xml";
    private static final String CONFIG_TAG = "Properties";
    private static final String SECTION_TAG = "Section";
    private static final String ITEM_TAG = "Property";
    
    // Recent property file
    private static final String RECENT_FILE = "rails.recent";
    
    // singleton configuration for ConfigManager
    private static final ConfigManager instance = new ConfigManager();
    
    // INSTANCE DATA
    
    // configuration items: replace with Multimap in Rails 2.0
    private final Map<String, List<ConfigItem>> configSections = new HashMap<String, List<ConfigItem>>();

    // recent data
    private final Properties recentData = new Properties();
    
    // profile storage
    private ConfigProfile activeProfile;
    
    /**
     * Initial configuration immediately after startup:
     * Setting of log4j and start logger
     * @param test if true configurations are setup for integration tests, false for productive use
     */
    private static void startlog4j() {
        
        // log4j settings
        String log4jSelection = System.getProperty(LOG4J_CLI_OPTION); 
        if (!Util.hasValue(log4jSelection)) {
            log4jSelection = LOG4J_CONFIG_FILE;
        }
        // Sets those settings
        System.setProperty("log4j.configuration", log4jSelection);
        System.out.println("log4j.configuration =  " + log4jSelection);
   
        // Activate logger
        log = Logger.getLogger(ConfigManager.class.getPackage().getName());
        log.debug("Activate log4j logging using configuration file = " + log4jSelection);
        
    }

    public static void initConfiguration(boolean test) {
        startlog4j();
        
        try {
            List<String> directories = new ArrayList<String>();
            directories.add(CONFIG_XML_DIR);
            // Find the config tag inside the the config xml file
            Tag configTag =
                    Tag.findTopTagInFile(CONFIG_XML_FILE, directories, CONFIG_TAG);
            log.debug("Opened config xml, filename = " + CONFIG_XML_FILE);
            instance.configureFromXML(configTag);
        } catch (ConfigurationException e) {
            log.error("Configuration error in setup of " + CONFIG_XML_FILE + ", exception = " + e);
        }
        
        if (test) {
            instance.initTest();
        } else {
            instance.init();
        }
    }


    /**
     * @return singleton instance of ConfigManager
     */
    public static ConfigManager getInstance() {
        return instance;
    }

    // private constructor to allow only creation of a singleton
    private ConfigManager() {}
    
    /** 
     * Reads the config.xml file that defines all config items
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
            
            // find sections
            List<Tag> sectionTags = tag.getChildren(SECTION_TAG);
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
            
    }

    
    public void finishConfiguration(GameManagerI parent)
            throws ConfigurationException {
        // do nothing
    }
    
    private void init() {
  
        // load recent data
        File recentFile = new File(SystemOS.get().getConfigurationFolder(false), RECENT_FILE);
        Util.loadProperties(recentData, recentFile);
        
        // define profiles
        ConfigProfile.readPredefined();
        ConfigProfile.readUser();
        
        // load root profile
        ConfigProfile.loadRoot();
        
        // change to start profile (cli, recent or default)
        changeProfile(ConfigProfile.getStartProfile());

        initVersion();
    }

    private void initTest() {
        ConfigProfile.loadRoot();
        activeProfile = ConfigProfile.loadTest();
        initVersion();
    }

    private void initVersion() {
        // TODO: Check if this is the right place for this
        /* Load version number and develop flag */
        Properties versionNumber = new Properties();
        Util.loadPropertiesFromResource(versionNumber, "version.number");

        String version = versionNumber.getProperty("version");
        if (Util.hasValue("version")) {
            Game.setVersion(version);
        }
            
        String develop = versionNumber.getProperty("develop");
        if (Util.hasValue(develop)) {
            Game.setDevelop(develop != "");
        }
    }

    String getValue(String key, String defaultValue) {

        // get value from active profile (this escalates)
        String value = activeProfile.getProperty(key);
        if (Util.hasValue(value)) {
            return value.trim();
        } else {
            return defaultValue;
        }
    }
   
    public String getActiveProfile() {
        return activeProfile.getName();
    }
    
    public boolean IsActiveUserProfile() {
        return activeProfile.getType() == ConfigProfile.Type.USER;
    }

    public List<String> getProfiles() {
        // sort and convert to names
        List<ConfigProfile> profiles = new ArrayList<ConfigProfile>(ConfigProfile.getProfiles());
        Collections.sort(profiles);
        List<String> profileNames = new ArrayList<String>();
        for (ConfigProfile profile:profiles){
            profileNames.add(profile.getName());
        }
        return profileNames;
    }
    
    public Map<String, List<ConfigItem>> getConfigSections() {
        return configSections;
    }
    
    public int getMaxElementsInPanels() {
        int maxElements = 0;
        for (List<ConfigItem> panel:configSections.values()) {
            maxElements = Math.max(maxElements, panel.size());
        }
        log.debug("Configuration sections with maximum elements of " + maxElements);
        return maxElements;
    }

    private void changeProfile(ConfigProfile profile) {
        activeProfile = profile;
        activeProfile.makeActive();

        // define configItems
        for (List<ConfigItem> items:configSections.values()) {
            for (ConfigItem item:items) {
                item.setCurrentValue(getValue(item.name, null));
            }
        }
    }
    
    public void changeProfile(String profileName) {
        changeProfile(ConfigProfile.getProfile(profileName));
    }
    
    /**
     * updates the user profile according to the changes in configItems
     */
    public boolean saveProfile(boolean applyInitMethods) {
        for (List<ConfigItem> items:configSections.values()) {
            for (ConfigItem item:items) {
                // if item has changed ==> change profile and call init Method
                if (item.hasChanged()) {
                    activeProfile.setProperty(item.name, item.getNewValue());
                    log.debug("User properties for = " + item.name + " set to value = " + item.getCurrentValue());
                    item.callInitMethod(applyInitMethods);
                    item.resetValue();
                }
            }
        }
        return activeProfile.store();
    }

    public boolean saveNewProfile(String name, boolean applyInitMethods) {
        activeProfile = activeProfile.deriveUserProfile(name);
        return saveProfile(applyInitMethods);
    }
    
    String getRecent(String key) {
        // get value from active profile (this escalates)
        String value = recentData.getProperty(key);
        if (Util.hasValue(value)) {
            return value.trim();
        } else {
            return null;
        }
    }
    
    boolean storeRecent(String key, String value) {
        // check conditions
        if (key == null) return false;
        if (getRecent(key) == null || !getRecent(key).equals(value) ) {
            if (value == null) {
                recentData.remove(key);
            } else {
                recentData.setProperty(key, value);
            }
            File recentFile = new File(SystemOS.get().getConfigurationFolder(true), RECENT_FILE);
            return Util.storeProperties(recentData, recentFile);
        }
        // nothing has changed
        return true;
    }
    
}
