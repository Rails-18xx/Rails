package net.sf.rails.common;

import com.google.common.base.Objects;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;
import lombok.Getter;

/**
 * GameInfo holds basic information about the game, such as:
 * 1. List of available game names.
 * 2. Min. and Max. players for the game.
 * 3. Game credits.
 */
public class GameInfo implements Comparable<GameInfo> {

    @Getter
    private final int minPlayers;

    @Getter
    private final int maxPlayers;

    @Getter
    private final String name;

    @Getter
    private final String note;

    @Getter
    private final String description;

    @Getter
    private final int ordering;

    private GameInfo(int minPlayers, int maxPlayers, String name, String note, String description, int ordering) {
        super();

        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.name = name;
        this.note = note;
        this.description = description;
        this.ordering = ordering;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final GameInfo other = (GameInfo) obj;
        return Objects.equal(this.name, other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public int compareTo(GameInfo other) {
        return Ints.compare(this.ordering, other.ordering);
    }

    public static GameInfo findGame(Iterable<GameInfo> gameList, String gameName) {
        return Streams.stream(gameList)
                .filter(game -> Objects.equal(game.name, gameName))
                .findFirst()
                .orElse(null);
    }

    public static Builder builder() {
        return new GameInfo.Builder();
    }

    public static class Builder {
        private int minPlayers;
        private int maxPlayers;
        private String name;
        private String note;
        private String description;
        private int ordering;

        private Builder() {
            // do nothing
        }

        public Builder withOrdering(int ordering) {
            this.ordering = ordering;

            return this;
        }

        public Builder withMinPlayers(int minPlayers) {
            this.minPlayers = minPlayers;

            return this;
        }

        public Builder withMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;

            return this;
        }

        public Builder withName(String name) {
            this.name = name;

            return this;
        }

        public Builder withNote(String note) {
            this.note = note;

            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;

            return this;
        }

        public GameInfo build() {
            return new GameInfo(minPlayers, maxPlayers, name, note, description, ordering);
        }
    }
}
