package net.sf.rails.common;

import lombok.Getter;

import java.util.List;

public class GameData {
    private final GameInfo game;

    @Getter
    private final GameOptionsSet gameOptions;

    @Getter
    private final List<String> players;

    private GameData(GameInfo game, GameOptionsSet gameOptions, List<String> players) {
        super();

        this.game = game;
        this.gameOptions = gameOptions;
        this.players = players;
    }

    public static GameData create(GameInfo game, GameOptionsSet.Builder gameOptions, List<String> players) {
        return new GameData(game, gameOptions.withNumberOfPlayers(players.size()).build(), players);
    }

    public String getGameName() {
        return game.getName();
    }
}
