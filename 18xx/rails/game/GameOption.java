/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/GameOption.java,v 1.12 2010/05/08 13:56:30 evos Exp $ */
package rails.game;

import java.util.*;

import rails.util.LocalText;

public class GameOption {

    private String name;
    private String parametrisedName;
    private boolean isBoolean = false;
    private String type;
    private String defaultValue = null;
    private List<String> allowedValues = null;
    private String[] parm = null;

    private static Map<String, GameOption> optionsMap =
            new HashMap<String, GameOption>();

    public static final String OPTION_VALUE_YES = "yes";
    public static final String OPTION_VALUE_NO = "no";

    // A default option that will always be set
    public static final String NUMBER_OF_PLAYERS = "NumberOfPlayers";

    // Some other common game options
    public static final String VARIANT = "Variant";


    public GameOption(String name, String[] parameters) {
    	this.name = name;
    	if (parameters != null) parm = parameters.clone();
        parametrisedName = constructParametrisedName (name, parameters);
        optionsMap.put(parametrisedName, this);
    }

    public void setType(String type) {
        this.type = type;
        if (type.equalsIgnoreCase("toggle")) {
            isBoolean = true;
            allowedValues = new ArrayList<String>();
            allowedValues.add("yes");
            allowedValues.add("no");
        }
    }

    public String getName() {
        return parametrisedName;
    }

    public String getLocalisedName() {
        return LocalText.getText(name, (Object[]) parm);
    }

    public String getType() {
        return type;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    /*
    public void setParameters(String[] parameters) {
        parm = parameters.clone();
    }
    */

    public void setAllowedValues(List<String> values) {
        allowedValues = values;
    }

    public void setAllowedValues(String[] values) {
        allowedValues = new ArrayList<String>();
        for (String value : values) {
            allowedValues.add(value);
        }
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public boolean isValueAllowed(String value) {
        return allowedValues == null || allowedValues.contains(value);
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        if (defaultValue != null) {
            return defaultValue;
        } else if (isBoolean) {
            return "no";
        } else if (allowedValues != null && !allowedValues.isEmpty()) {
            return allowedValues.get(0);
        } else {
            return "";
        }
    }

    public static GameOption getByName(String name) {
        return optionsMap.get(name);
    }

    /** Get GameOption Value as Boolean Value */
    public static boolean convertValueToBoolean(String value) {
        return value != null
            && OPTION_VALUE_YES.equalsIgnoreCase(value);
    }

    public static String constructParametrisedName (String optionName,
    		String[] optionNameParameters) {
        String parametrisedName = optionName;
    	if (optionNameParameters != null) {
    		for (String parameter : optionNameParameters) {
    			parametrisedName += "_" + parameter;
    		}
    	}
    	return parametrisedName;
    }
}
