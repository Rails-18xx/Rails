package net.sf.rails.common;

import com.google.common.base.Objects;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;
import lombok.*;

/**
 * GameInfo holds basic information about the game, such as:
 * 1. List of available game names.
 * 2. Min. and Max. players for the game.
 * 3. Game credits.
 */
@Builder(setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
}
