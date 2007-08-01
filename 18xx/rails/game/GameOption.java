package rails.game;

import java.util.ArrayList;
import java.util.List;

public class GameOption {
	
    private String name;
    private boolean isBoolean = false;
    private String type;
    private String defaultValue = null;
    private List<String> allowedValues = null;

    public GameOption (String name) {
        this.name = name;
    }
    
    public void setType (String type) {
    	this.type = type;
    	if (type.equalsIgnoreCase("toggle")) {
            isBoolean = true;
    		allowedValues = new ArrayList<String>();
    		allowedValues.add("yes");
    		allowedValues.add("no");
    	}
    }
    
    public String getName() {
    	return name;
    }
    
    public String getType() {
    	return type;
    }
    
    public boolean isBoolean () {
    	return isBoolean;
    }
    
    public void setAllowedValues (List<String> values) {
        allowedValues = values;
    }
    
    public void setAllowedValues (String[] values) {
    	allowedValues = new ArrayList<String>();
    	for (String value : values) {
    		allowedValues.add(value);
    	}
    }
    
    public List<String> getAllowedValues() {
        return allowedValues;
    }
    
    public boolean isValueAllowed (String value) {
    	return allowedValues == null || allowedValues.contains(value);
    }
    
    public void setDefaultValue (String defaultValue) {
    	this.defaultValue = defaultValue;
    }
    
    public String getDefaultValue () {
        if (defaultValue != null) {
            return defaultValue;
        } else {
            return "";
        }
    }

}
