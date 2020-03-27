package net.sf.rails.common;

import java.util.List;

public class GameData {
    private final GameInfo game;
    private final GameOptionsSet gameOptions;
    private final List<String> players;
    private final int seed;
    
    private GameData(GameInfo game, GameOptionsSet gameOptions, List<String> players, int seed) {
        this.game = game;
        this.gameOptions = gameOptions;
        this.players = players;
        this.seed = seed;
    }
    
    public static GameData create(GameInfo game, GameOptionsSet.Builder gameOptions, List<String> players, int seed) {
        return new GameData(game, gameOptions.build(players.size()), players, seed);
    }
    
    public String getGameName() {
        return game.getName();
    }
    
    public GameOptionsSet getGameOptions() {
        return gameOptions;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public int getSeed() {
    	return seed;
    }
}
