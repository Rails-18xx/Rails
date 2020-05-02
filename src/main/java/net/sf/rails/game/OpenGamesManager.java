package net.sf.rails.game;

import java.util.HashMap;
import java.util.Map;

import net.sf.rails.ui.swing.GameUIManager;

public class OpenGamesManager {

    private static final OpenGamesManager instance = new OpenGamesManager();

    private final Map<String, GameUIManager> openGames = new HashMap<>();
    private OpenGamesManager() { }

    public static OpenGamesManager getInstance() {
        return instance;
    }

    public GameUIManager getGame(String id) {
        return openGames.get(id);
    }

    public void addGame(GameUIManager gammeUIManager) {
        // TODO: fix key
        openGames.put(gammeUIManager.getRoot().getId(), gammeUIManager);
    }

    public void removeGame(GameUIManager gameUIManager) {
        openGames.remove(gameUIManager.getRoot().getId());
    }

    public int countOfOpenGames() {
        return openGames.size();
    }

}
