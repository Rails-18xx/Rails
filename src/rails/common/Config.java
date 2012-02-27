package rails.common;

import rails.game.GameManager;
import rails.util.Util;

/**
 * Proxy class to the ConfigManager 
 */

public class Config {

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
        return getSpecific(key, GameManager.getInstance().getGameName());
    }

    
    
}
