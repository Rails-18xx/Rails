package net.sf.rails.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameData {
    private final GameInfo game;

    @Getter
    private final GameOptionsSet gameOptions;

    @Getter
    private final List<String> players;

    public static GameData create(GameInfo game, GameOptionsSet.Builder gameOptions, List<String> players) {
        return new GameData(game, gameOptions.withNumberOfPlayers(players.size()).build(), players);
    }

    public String getGameName() {
        return game.getName();
    }
}
