package net.sf.rails.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.Util;

/**
 * Proxy class to the ConfigManager
 */

public final class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    /**
    * @return version id (including a "+" attached if development)
    */
    public static String getVersion() {
        return ConfigManager.getInstance().getVersion();
    }

    /**
     * @return true if development version
     */
    public static boolean isDevelop() {
        return ConfigManager.getInstance().isDevelop();
    }

    public static String getBuildDate() {
        return ConfigManager.getInstance().getBuildDate();
    }

    /**
     * Configuration option (default value is empty string)
     */
    public static String get(String key) {
        return ConfigManager.getInstance().getValue(key, "");
    }

    /**
     * Configuration option with default value
     */
    public static String get(String key, String defaultValue) {
        return ConfigManager.getInstance().getValue(key, defaultValue);
    }

    /**
     * Returns a boolean based on the config value (ie "yes", "no"). If the config value doesn't exist or is empty null is returned
     * @param key
     * @return
     */
    public static Boolean getBoolean(String key) {
        String boolStr = get(key);
        if ( StringUtils.isBlank(boolStr) ) {
            return null;
        }
        return Util.parseBoolean(boolStr);
    }

    /**
     * Returns a boolean based on the config value (ie "yes", "no"). If the config value doesn't exist or is empty then the default value is returned
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        Boolean bool = getBoolean(key);
        return bool != null ? bool : defaultValue;
    }

    public static Integer getInt(String key) {
        String intStr = get(key);
        if ( StringUtils.isBlank(intStr) ) {
            return null;
        }
        try {
            return Integer.valueOf(intStr);
        }
        catch (NumberFormatException e) {
            log.warn("Invalid value found in integer config {}: {}", key, intStr);
        }
        return null;
    }

    public static Integer getInt(String key, int defaultValue) {
        Integer intValue = getInt(key);
        return intValue != null ? intValue : defaultValue;
    }

    public static void set(String key, String value) {
        ConfigManager.getInstance().setValue(key, value);
    }

    public static void setBoolean(String key, boolean value) {
        set(key, value ? "yes" : "no");
    }

    /**
     * Configuration option: First tries to return {key}.{appendix}, if undefined returns {key}
     */
    public static String getSpecific(String key, String appendix) {
        String value = get(key + "." + appendix);
        if (Util.hasValue(value)) {
            return value;
        } else {
            return get(key);
        }
    }

    /**
     * Configuration option: First tries to return {key}.{gameName}, if undefined returns {key}
     */
    public static String getGameSpecific(String key) {
        return getSpecific(key, RailsRoot.getInstance().getGameName());
    }

    public static String getRecent(String key) {
        return ConfigManager.getInstance().getRecent(key);
    }

    public static boolean storeRecent(String key, String value) {
        return ConfigManager.getInstance().storeRecent(key, value);
    }

}


