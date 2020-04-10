package net.sf.rails.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.SortedSet;

/**
 * A class to store several GameOptions, including values
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameOptionsSet {

    @Getter
    private final Map<String, String> options;

    public String get(String option) {
        return options.get(option);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private final SortedSet<GameOption> options = Sets.newTreeSet();

        private int numberOfPlayers;

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

            return new GameOptionsSet(gameOptions);
        }
    }

}
