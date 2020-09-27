package net.sf.rails.common;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import net.sf.rails.game.RailsItem;
import net.sf.rails.util.Util;

import java.util.List;

public class GameOption implements Comparable<GameOption> {

    // Strings that define yes or no options
    public static final String OPTION_VALUE_YES = "yes";
    public static final String OPTION_VALUE_NO = "no";

    // Strings that define types
    public static final String OPTION_TYPE_SELECTION = "selection";
    public static final String OPTION_TYPE_TOGGLE = "toggle";

    /** For the random seed (as in SOH), which must be saved,
     * but should not appear as a selectable Game option */
    public static final String OPTION_TYPE_HIDDEN = "hidden";
    public static final String RANDOM_SEED = "RandomSeed";

    // A default option that will always be set
    public static final String NUMBER_OF_PLAYERS = "NumberOfPlayers";
    // Some other common game options
    public static final String VARIANT = "Variant";

    private final String name;

    private final String localisedName;

    private final boolean isBoolean;

    private final boolean isHidden;

    private final String defaultValue;
    
    private final List<String> allowedValues;

    private final int ordering;

    private String selectedValue;

    public GameOption(String name, String localisedName, boolean isBoolean, boolean isHidden,
                      String defaultValue, List<String> allowedValues, int ordering) {
        super();
        
        this.name = name;
        this.localisedName = localisedName;
        this.isBoolean = isBoolean;
        this.isHidden = isHidden;
        this.defaultValue = defaultValue;
        this.allowedValues = allowedValues;
        this.ordering = ordering;
    }

    public String getName() {
        return name;
    }

    public String getLocalisedName() {
        return localisedName;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public boolean isValueAllowed(String value) {
        return allowedValues.contains(value);
    }

    public String getSelectedValue() {
        if (selectedValue == null) {
            return defaultValue;
        } else {
            return selectedValue;
        }
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final GameOption other = (GameOption) obj;
        return Objects.equal(this.name, other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public int compareTo(GameOption other) {
        return ComparisonChain.start()
                .compare(this.ordering, other.ordering)
                .compare(this.name, other.name)
                .result();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String type = OPTION_TYPE_SELECTION;
        private String defaultValue = null;
        private List<String> allowedValues = null;
        private List<String> parameters = null;
        private int ordering = 0;

        private Builder(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public void setAllowedValues(Iterable<String> values) {
            allowedValues = ImmutableList.copyOf(values);
        }

        public void setParameters(Iterable<String> parameters) {
            this.parameters = ImmutableList.copyOf(parameters);
        }

        public void setOrdering(int ordering) {
            this.ordering = ordering;
        }

        private String getLocalisedName() {
            if (parameters == null || parameters.isEmpty()) {
                return LocalText.getText(name);
            }

            ImmutableList.Builder<String> localTextPars = ImmutableList.builder();
            for (String par : parameters) {
                localTextPars.add(LocalText.getText(par));
            }
            // TODO (Rails2.0): Change method signature in LocalText
            return LocalText.getText(name, localTextPars.build().toArray());
        }

        private String getFinalDefaultValue(Boolean isBoolean, List<String> finalAllowedValues) {
            if (defaultValue != null) {
                return defaultValue;
            } else if (isBoolean) {
                return OPTION_VALUE_NO;
            } else if (type.equalsIgnoreCase(GameOption.OPTION_TYPE_HIDDEN)) {
                return null;
            } else if (!allowedValues.isEmpty()) {
                return allowedValues.get(0);
            } else {
                return null;
            }
        }

        public GameOption build() {

            // use type information
            boolean isBoolean = false;
            boolean isHidden = false;
            List<String> finalAllowedValues = ImmutableList.of();
            if (type.equalsIgnoreCase(OPTION_TYPE_TOGGLE)) {
                isBoolean = true;
                finalAllowedValues = ImmutableList.of(OPTION_VALUE_YES, OPTION_VALUE_NO);
            } else if (type.equalsIgnoreCase(OPTION_TYPE_SELECTION)) {
                if (allowedValues == null) {
                    finalAllowedValues = ImmutableList.of();
                } else {
                    finalAllowedValues = allowedValues;
                }
            } else if (type.equalsIgnoreCase(OPTION_TYPE_HIDDEN)) {
                isHidden = true;
            }

            String parameterisedName = constructParameterisedName(name, parameters);
            String localisedName = getLocalisedName();
            String finalDefaultValue = getFinalDefaultValue(isBoolean, finalAllowedValues);

            return new GameOption(parameterisedName, localisedName, isBoolean, isHidden,
                    finalDefaultValue, finalAllowedValues, ordering);
        }
    }

    /**
     * Returns parameterised Name
     */
    public static String constructParameterisedName(String name, List<String> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            return name + "_" + Joiner.on("_").join(parameters);
        } else {
            return name;
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
     * Returns the boolean value of the gameOption in a game which contains the
     * RailItem If not defined as in OPTION_VALUE_YES, it returns false
     */
    public static boolean getAsBoolean(RailsItem item, String gameOption) {
        String value = getValue(item, gameOption);
        return value != null && OPTION_VALUE_YES.equalsIgnoreCase(value);
    }
}
