package rails.util;

import java.util.Arrays;
import java.util.List;
import rails.game.ConfigurationException;
import rails.util.Config.ConfigType;

/**
 * Defines an item used for the configuration of rails
 * T represents the value type
 */

public class ConfigItem {
    public final String name;
    public final ConfigType type;
    public final List<String> allowedValues;
    public final String formatMask;
    public final String helpText;
    
    ConfigItem(Tag tag) throws ConfigurationException {
        // check name and type (required)
        String name = tag.getAttributeAsString("name");
        if (Util.hasValue(name)) {
            this.name = name;
        } else {
            throw new ConfigurationException("Missing name for configuration item");
        }
        String type = tag.getAttributeAsString("type");
        if (Util.hasValue(type)) {
            this.type = ConfigType.valueOf(type);
        } else {
            throw new ConfigurationException("Missing or invalid type for configuration item");
        }
        // optional: list of allowed values
        String valueString = tag.getAttributeAsString("values");
        if (Util.hasValue(valueString)) {
            allowedValues = Arrays.asList(valueString.split(","));
        } else {
            allowedValues = null;
        }
        // optional: formatMask
        formatMask = tag.getAttributeAsString("formatMask");

        // optional: helpText
        helpText = tag.getAttributeAsString("formatMask");
    }
    
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("Configuration Item: name = " + name + ", type = " + type);
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
