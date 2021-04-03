package net.sf.rails.util;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.PossibleAction;

import net.sf.rails.common.GameData;


/**
 * Combines all elements for gameIO
 */
class GameIOData {

    private GameData gameData;
    private String version;
    private String date;
    private long fileVersionID;
    private List<PossibleAction> actions;
    
    GameIOData(GameData gameData, String version, String date, Long fileVersionID, List<PossibleAction> actions) {
        this.gameData = gameData;
        this.version = version;
        this.date = date;
        this.fileVersionID = fileVersionID;
        this.actions = new ArrayList<>(actions);
    }
    
    GameIOData() {}
    
    
    void setGameData(GameData gameData) {
        this.gameData = gameData;
    }
    
    GameData getGameData() {
        return gameData;
    }
    
    void setVersion(String version) {
        this.version = version;
    }
    
    String getVersion() {
        return version;
    }
    
    void setDate(String date) {
        this.date = date;
    }
    
    String getDate() {
        return date;
    }
    
    void setFileVersionID(long fileVersionID) {
        this.fileVersionID = fileVersionID;
    }
    
    long getFileVersionID() {
        return fileVersionID;
    }
    
    void setActions(List<PossibleAction> actions) {
        this.actions = new ArrayList<>(actions);
    }

    List<PossibleAction> getActions() {
        return actions;
    }

    String metaDataAsText() {
        StringBuilder s = new StringBuilder();
        s.append("Rails saveVersion = ").append(version).append("\n");
        s.append("File was saved at ").append(date).append("\n");
        s.append("Saved versionID=").append(fileVersionID).append("\n");
        s.append("Save game=").append(gameData.getGameName()).append("\n");
        return s.toString();
    }

    String gameOptionsAsText() {
        StringBuilder s = new StringBuilder();
        for (String key : gameData.getGameOptions().getOptions().keySet()) {
            s.append("Option ").append(key).append("=").append(gameData.getGameOptions().get(key)).append("\n");
        }
        return s.toString();
    }

    String playerNamesAsText() {
        StringBuilder s = new StringBuilder();
        int i=1;
        for (String player : gameData.getPlayers()) {
            s.append("Player ").append(i++).append(": ").append(player).append("\n");
        }
        return s.toString();
    }

}
