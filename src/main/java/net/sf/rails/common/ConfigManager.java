package net.sf.rails.common;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.SystemOS;
import net.sf.rails.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;


/**
 * ConfigManager is a utility class that collects all functions
 * used to define and control configuration options
 * <p>
 * It is a rewrite of the previously used static class Config
 */
public class ConfigManager implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

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

    // version string and development flag
    private String version = "unknown";

    private boolean develop = false;

    private String buildDate = "unknown";

    // configuration items: replace with Multimap in Rails 2.0
    private final Map<String, List<ConfigItem>> configSections = new TreeMap<>();

    // recent data
    private final Properties recentData = new Properties();

    // profile storage
    private ConfigProfile activeProfile;
    private Map<String, String> transientConfig = new HashMap<>();

    private ConfigManager() {
        // do nothing
    }

    public static void initConfiguration(boolean test) {
        try {
            // Find the config tag inside the the config xml file
            // the last arguments refers to the fact that no GameOptions are required
            Tag configTag = Tag.findTopTagInFile(CONFIG_XML_FILE, CONFIG_XML_DIR, CONFIG_TAG, null);
            log.debug("Opened config xml, filename = " + CONFIG_XML_FILE);
            instance.configureFromXML(configTag);
        } catch (ConfigurationException e) {
            log.error("Configuration error in setup of " + CONFIG_XML_FILE, e);
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

    /**
     * Reads the config.xml file that defines all config items
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // find sections
        List<Tag> sectionTags = tag.getChildren(SECTION_TAG);
        if (sectionTags != null) {
            for (Tag sectionTag : sectionTags) {
                // find name attribute
                String sectionName = sectionTag.getAttributeAsString("name");
                if (!Util.hasValue(sectionName)) continue;

                // find items
                List<Tag> itemTags = sectionTag.getChildren(ITEM_TAG);
                if (itemTags == null || itemTags.size() == 0) continue;
                List<ConfigItem> sectionItems = new ArrayList<>();
                for (Tag itemTag : itemTags) {
                    ConfigItem configItem = new ConfigItem(itemTag);
                    sectionItems.add(configItem);
                }
                configSections.put(sectionName, sectionItems);
            }
        }
    }

    public void finishConfiguration(RailsRoot parent)
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
        Util.loadPropertiesFromResource(versionNumber, "git.properties");

        String sVersion = versionNumber.getProperty("git.build.version");
        if (Util.hasValue(sVersion)) {
            this.version = sVersion;
        }

        this.develop = StringUtils.isNotBlank(versionNumber.getProperty("develop"));

        String sBuildDate = versionNumber.getProperty("buildDate");
        if (Util.hasValue(sBuildDate)) {
            this.buildDate = sBuildDate;
        }
    }

    public boolean isDevelop() {
        return develop;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public Map<String, List<ConfigItem>> getConfigSections() {
        return configSections;
    }

    /**
     * @return version id (including a "+" attached if development)
     */
    public String getVersion() {
        if (develop) {
            return version + "+";
        } else {
            return version;
        }
    }

    public String getValue(String key, String defaultValue) {
        if (transientConfig.containsKey(key)) {
            return transientConfig.get(key);
        }

        // get value from active profile (this escalates)
        String value = activeProfile.getProperty(key);
        if (Util.hasValue(value)) {
            return value.trim();
        } else {
            return defaultValue;
        }
    }

    public void setValue(String key, String value) {
        transientConfig.put(key, value);
    }

    public String getActiveProfile() {
        return activeProfile.getName();
    }

    public String getActiveParent() {
        return activeProfile.getParent().getName();
    }

    public boolean IsActiveUserProfile() {
        return activeProfile.getType() == ConfigProfile.Type.USER;
    }

    public List<String> getProfiles() {
        // sort and convert to names
        List<ConfigProfile> profiles = new ArrayList<>(ConfigProfile.getProfiles());
        Collections.sort(profiles);
        List<String> profileNames = new ArrayList<>();
        for (ConfigProfile profile : profiles) {
            profileNames.add(profile.getName());
        }
        return profileNames;
    }

    public int getMaxElementsInPanels() {
        int maxElements = 0;
        for (List<ConfigItem> panel : configSections.values()) {
            maxElements = Math.max(maxElements, panel.size());
        }
        log.debug("Configuration sections with maximum elements of {}", maxElements);
        return maxElements;
    }

    private void changeProfile(ConfigProfile profile) {
        activeProfile = profile;
        activeProfile.makeActive();

        // define configItems
        for (List<ConfigItem> items : configSections.values()) {
            for (ConfigItem item : items) {
                // TODO: should we ignore isGameRelated?
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
        log.debug("saving profile now");
        for (List<ConfigItem> items : configSections.values()) {
            for (ConfigItem item : items) {
                if (item.isGameRelated) {
                    if (StringUtils.isNotBlank(item.getNewValue())) {
                        transientConfig.put(item.name, item.getNewValue());
                    } else {
                        transientConfig.remove(item.name);
                    }
                    continue;
                }
                // if item has changed ==> change profile and call init Method
                if (item.hasChanged()) {
                    activeProfile.setProperty(item.name, item.getNewValue());
                    log.debug("User properties for = {} set to value = {}", item.name, item.getCurrentValue());
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

    public boolean deleteActiveProfile() {
        if (activeProfile.delete()) {
            activeProfile = activeProfile.getParent();
            return true;
        } else {
            return false;
        }
    }

    public String getRecent(String key) {
        // get value from active profile (this escalates)
        String value = recentData.getProperty(key);
        if (Util.hasValue(value)) {
            return value.trim();
        } else {
            return null;
        }
    }

    public boolean storeRecent(String key, String value) {
        // check conditions
        if (key == null) return false;
        if (getRecent(key) == null || !getRecent(key).equals(value)) {
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
