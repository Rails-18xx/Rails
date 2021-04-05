package net.sf.rails.common;

import java.util.ArrayList;
import java.util.List;

public class GameData {
    private final GameInfo game;

    private final GameOptionsSet gameOptions;

    private final List<String> players;

    private GameData(GameInfo game, GameOptionsSet gameOptions, List<String> players) {
        super();

        this.game = game;
        this.gameOptions = gameOptions;
        this.players = new ArrayList<>(players);
    }

    public static GameData create(GameInfo game, GameOptionsSet.Builder gameOptions, List<String> players) {
        return new GameData(game, gameOptions.withNumberOfPlayers(players.size()).build(), players);
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
}
