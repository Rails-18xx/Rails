package rails.common;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;


public class GameData {
    private final GameInfo game;
    private final Map<String, String> gameOptions;
    private final List<String> players;
    
    private GameData(GameInfo game, Map<String, String> gameOptions, List<String> players) {
        this.game = game;
        this.gameOptions = ImmutableMap.copyOf(gameOptions);
        this.players = players;
    }
    
    @Deprecated
    public static GameData createLegacy(GameInfo game, Map<String, String> gameOptions, List<String> players) {
        return new GameData(game, gameOptions, players);
    }
    
    public static GameData create(GameInfo game, Map<GameOption, String> gameOptions, List<String> players) {
        ImmutableMap.Builder<String, String> legacyOptions = ImmutableMap.builder();
        for (GameOption option:gameOptions.keySet()) {
            legacyOptions.put(option.getName(), gameOptions.get(option));

        }
        // TODO (Rails2.0): Is this the correct place?
        legacyOptions.put(GameOption.NUMBER_OF_PLAYERS,
                String.valueOf(players.size()));
        return createLegacy(game, legacyOptions.build(), players);
    }
    
    public String getGameName() {
        return game.getName();
    }
    
    public Map<String, String> getGameOptions() {
        return gameOptions;
    }
    
    public List<String> getPlayers() {
        return players;
    }
}
