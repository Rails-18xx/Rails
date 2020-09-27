package net.sf.rails.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.sf.rails.util.Util;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class to store several GameOptions, including values
 */
public class GameOptionsSet {

    private final Map<String, String> options;

    private GameOptionsSet(Map<String, String> options) {
        super();

        this.options = options;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String get(String option) {
        return options.get(option);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<GameOption> options = new TreeSet<>();

        private int numberOfPlayers;

        private Builder() {
            // do nothing
        }

        public Builder withNumberOfPlayers(int numberOfPlayers) {
            this.numberOfPlayers = numberOfPlayers;

            return this;
        }

        public Builder withOption(GameOption option) {
            this.options.add(option);

            return this;
        }

        @Deprecated
        public ImmutableList<GameOption> getOptions() {
            return ImmutableList.copyOf(options);
        }

        public GameOptionsSet build() {
            final Map<String, String> gameOptions = Maps.newLinkedHashMap();

            gameOptions.put(GameOption.NUMBER_OF_PLAYERS, Integer.toString(numberOfPlayers));

            for (GameOption option : options) {
                gameOptions.put(option.getName(), option.getSelectedValue());
            }

            // Added EV sep 2020 for SOH:
            // create a random seed that persists if the game is saved and reloaded.
            // To make this work, the GameOption "RandomSeed" with type="hidden"
            // must be defined in GameOption.xml
            if (gameOptions.containsKey(GameOption.RANDOM_SEED)) {
                String seed = gameOptions.get(GameOption.RANDOM_SEED);
                if (!Util.hasValue(seed)) {  // Skip setting it on reloading
                    gameOptions.put(GameOption.RANDOM_SEED, String.valueOf(System.currentTimeMillis()));
                }
            }

            return new GameOptionsSet(gameOptions);
        }
    }

}
