package rails.util;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.ConfigurationException;

/**
 * Defines an item used for the configuration of rails
 * T represents the value type
 */

public final class ConfigItem {

    protected static Logger log =
        Logger.getLogger(ConfigItem.class.getPackage().getName());

    /**
     * Defines possible types (Java classes used as types in ConfigItem below
     */
    public static enum ConfigType {
        INTEGER, FLOAT, STRING, BOOLEAN, LIST, DIRECTORY, COLOR;
    }
    
    // static attributes
    public final String name;
    public final ConfigType type;
    public final List<String> allowedValues;
    public final String formatMask;
    public final String helpText;
    
    // dynamic attributes
    private String newValue;
    
    ConfigItem(Tag tag) throws ConfigurationException {
        // check name and type (required)
        String name = tag.getAttributeAsString("name");
        if (Util.hasValue(name)) {
            this.name = name;
        } else {
            throw new ConfigurationException("Missing name for configuration item");
        }
        // optional: list of allowed values
        String valueString = tag.getAttributeAsString("values");
        if (Util.hasValue(valueString)) {
            allowedValues = Arrays.asList(valueString.split(","));
            this.type = ConfigType.LIST;
        } else {
            allowedValues = null;
            String type = tag.getAttributeAsString("type");
            if (Util.hasValue(type)) {
                try {
                    this.type = ConfigType.valueOf(type.toUpperCase());
                } catch (Exception e) {
                    throw new ConfigurationException("Missing or invalid type for configuration item");
                }
            } else {
                throw new ConfigurationException("Missing or invalid type for configuration item");
            }
            if (this.type == ConfigType.LIST) {
                throw new ConfigurationException("No values defined for LIST config item");
            }
        }
        
        // optional: formatMask
        formatMask = tag.getAttributeAsString("formatMask");

        // optional: helpText
        helpText = tag.getAttributeAsString("helpText");
        
        newValue = null;
    }
    
    public boolean hasNewValue() {
        return (newValue != null);
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        if (newValue == null || newValue.equals(getConfigValue())) {
            this.newValue = null;
        } else {
            this.newValue = newValue;
        }
        log.debug("ConfigItem " + name + " set to new value " + this.newValue);
    }
    
    public String getCurrentValue() {
        if (newValue != null) return newValue;
        return getConfigValue();
    }
    
    public String getConfigValue() {
        return Config.get(this.name);
    }
    
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("Configuration Item: name = " + name + ", type = " + type);
        s.append(", config value = " + getConfigValue());
        s.append(", current value = " + getCurrentValue());
        if (allowedValues != null) {
            s.append(", allowedValues = " + allowedValues);
        }
        if (formatMask != null) {
            s.append(", formatMask = " + formatMask);
        }
        if (helpText != null) {
            s.append(", helpText = " + helpText);
        }
        
        return s.toString();
    }
    
}
