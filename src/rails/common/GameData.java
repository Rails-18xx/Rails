package rails.common;

import java.util.List;
import java.util.Map;

public class GameData {

    private final String gameName;
    private final Map<String, String> gameOptions;
    private final List<String> players;
    
    public GameData(String gameName, Map<String, String> gameOptions, List<String> players) {
        this.gameName = gameName;
        this.gameOptions = gameOptions;
        this.players = players;
    }
    
    public String getGameName() {
        return gameName;
    }
    
    public Map<String, String> getGameOptions() {
        return gameOptions;
    }
    
    public List<String> getPlayers() {
        return players;
    }
}
