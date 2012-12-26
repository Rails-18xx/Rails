package rails.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rails.game.RailsItem;
import rails.util.Util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class GameOption {

    // A default option that will always be set
    public static final String NUMBER_OF_PLAYERS = "NumberOfPlayers";
    // Some other common game options
    public static final String VARIANT = "Variant";

    // A regex to match parameters against
    private static final Pattern PATTERN = Pattern.compile("\\{(.*)\\}");
    // Strings that define yes or no options
    private static final String OPTION_VALUE_YES = "yes";
    
    // Static Data
    private final String name;
    
    // Dynamic Data
    private boolean isBoolean = false;
    private String type;
    private String defaultValue = null;
    private List<String> allowedValues = null;
    private List<String> parameters = null;
    private String parametrisedName;

    public GameOption(String name) {
        this.name = name;
        parametrisedName = name;
        parameters = ImmutableList.of();
    }
    
    public GameOption(String name, String[] parameters) {
        this.name = name;
        this.setParameters(parameters);
    }
    
    public void setParameters(String[] parameters) {
    	if (parameters != null) {
    	    this.parameters = ImmutableList.copyOf(parameters);
    	    parametrisedName = Joiner.on("_").join(name, parameters);
    	} else {
    	    this.parameters = ImmutableList.of();
    	    parametrisedName = name;
    	}
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
        ImmutableList.Builder<String> localTextPars = ImmutableList.builder();
        for (String par:parameters) {
            Matcher m = PATTERN.matcher(par);
            if (m.matches()) {
                localTextPars.add(LocalText.getText(m.group(1)));
            }
        }
        // TODO (Rails2.0): Chane method signature in LocalText
        return LocalText.getText(name, (Object[]) localTextPars.build().toArray());
    }

    public String getType() {
        return type;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

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
    
    /**
     * Returns the value of the gameOption in a game which contains the RailItem
     */
    public static String getValue(RailsItem item, String gameOption) {
        // check the System properties for overwrites first
        if (Util.hasValue(System.getProperty(gameOption))) {
            return System.getProperty(gameOption);
        } else {
            return item.getRoot().getGameOptions().get(gameOption);
        }
    }
    
    /**
     * Returns the boolean value of the gameOption in a game which contains the RailItem
     * If not defined as in OPTION_VALUE_YES, it returns false 
     */
    public static boolean getAsBoolean(RailsItem item, String gameOption) {
        String value = getValue(item, gameOption);
        return value != null && OPTION_VALUE_YES.equalsIgnoreCase(value);
    }

}
