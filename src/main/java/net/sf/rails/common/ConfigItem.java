package net.sf.rails.common;

import java.util.Arrays;
import java.util.List;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Defines an item used for the configuration of rails
 * T represents the value type
 */

public final class ConfigItem {

    private static final Logger log =
            LoggerFactory.getLogger(ConfigItem.class);

    /**
     * Defines possible types (Java classes used as types in ConfigItem below
     */
    public static enum ConfigType {
        BOOLEAN, INTEGER, PERCENT, STRING, LIST, FONT, DIRECTORY, FILE, COLOR;
    }
    
    // static attributes
    public final String name;
    public final ConfigType type;
    public final List<String> allowedValues;
    public final String formatMask;
    
    // method call attributes
    private final String initClass;
    private final String initMethod;
    private final boolean alwaysCallInit;
    private final boolean initParameter;
    
    // dynamic attributes
    private String newValue;
    private String currentValue;
    
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
                    throw new ConfigurationException("Missing or invalid type for configuration item, exception = " + e);
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
    
        // optional: init method attributes
        initClass = tag.getAttributeAsString("initClass");
        initMethod = tag.getAttributeAsString("initMethod");
        alwaysCallInit = tag.getAttributeAsBoolean("alwaysCallInit",false);
        initParameter = tag.getAttributeAsBoolean("initParameter", false);
        
        // intialize values
        currentValue = null;
        newValue = null;
    }
    
    
    public boolean hasChanged() {
        if (newValue == null) return false;
        return !getCurrentValue().equals(newValue);
    }
    
    public String getValue() {
        if (hasChanged()) {
            return getNewValue();
        } else {
            return getCurrentValue();
        }
    }
    
    public String getCurrentValue() {
        if (currentValue == null) return "";
        return currentValue;
    }
    
    public void setCurrentValue(String value) {
        currentValue = value;
        newValue = null;
    }
    
    @Deprecated
    public boolean hasNewValue() {
        return (newValue != null);
    }
    
    public String getNewValue() {
        if (newValue == null) return "";
        return newValue;
    }
    
    public void setNewValue(String value) {
        if (value == null || value.equals("") || value.equals(currentValue)) {
            newValue = null;
        } else {
            newValue = value;
        }
        log.debug("ConfigItem " + name + " set to new value " + newValue);
    }
    
    public void resetValue() {
        if (hasChanged()) {
            currentValue = newValue;
            newValue = null;
        }
    }
    
    /**
     * @param applyInitMethod Specifies whether init should be called. Can be overruled
     * by an additional tag alwaysCallInit
     */
    void callInitMethod(boolean applyInitMethod) {
        if (!applyInitMethod && !alwaysCallInit) return;
        if (initClass == null || initMethod == null) return;
        
        // call without parameter
        try {
            Class<?> clazz = Class.forName(initClass);
            
            if (initParameter) {
                clazz.getMethod(initMethod, String.class).invoke(null, newValue);
               
            } else {
                clazz.getMethod(initMethod).invoke(null);
            }
        } catch (Exception e) {
            log.error("Config profile: cannot call initMethod, Exception = " + e.toString());
        }
    }
    
    
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("Configuration Item: name = " + name + ", type = " + type);
        s.append(", current value = " + getCurrentValue()) ;
        s.append(", new value = " + getNewValue());
        if (allowedValues != null) {
            s.append(", allowedValues = " + allowedValues);
        }
        if (formatMask != null) {
            s.append(", formatMask = " + formatMask);
        }
        
        return s.toString();
    }
    
    
    
}
