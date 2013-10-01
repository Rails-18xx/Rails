package rails.common;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

/**
 * GameInfo holds basic information about the game, such as:
 * 1. List of available game names.
 * 2. Min. and Max. players for the game.
 * 3. Game credits.
 */
public class GameInfo implements Comparable<GameInfo> {

    private final int minPlayers;
    private final int maxPlayers;

    private final String name;
    private final String note;
    private final String description;
    
    private final int ordering;
    
    private GameInfo(int minPlayers, int maxPlayers, String name, String note,
            String description, int ordering) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.name = name;
        this.note = note;
        this.description = description;
        this.ordering = ordering;
    }
    
    @Deprecated
    public static GameInfo createLegacy(String name) {
        return new GameInfo(0, 0, name, null, null, 0);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }

    public String getDescription() {
        return description;
    }
    
    public int getOrdering() {
        return ordering;
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
        for (GameInfo game:gameList) { 
            if (Objects.equal(game.name,gameName)) {
                return game;
            }
        }
        return null;
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

        private Builder() {};
        
        public GameInfo build(int ordering) {
            return new GameInfo(minPlayers, maxPlayers, name, note, description, ordering);
        }
        
        public void setMinPlayers(int minPlayers) {
            this.minPlayers = minPlayers;
        }
        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setNote(String note) {
            this.note = note;
        }
        public void setDescription(String description) {
            this.description = description;
        }
    }

}
