package rails.util;

import java.util.List;

import rails.common.GameData;
import rails.game.action.PossibleAction;

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
        this.actions = actions;
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
        this.actions = actions;
    }

    List<PossibleAction> getActions() {
        return actions;
    }
    
    String metaDataAsText() {
        StringBuilder s = new StringBuilder();
        s.append("Rails saveVersion = " + version + "\n");
        s.append("File was saved at " + date + "\n");
        s.append("Saved versionID=" + fileVersionID + "\n");
        s.append("Save game=" + gameData.getGameName() + "\n");
        return s.toString();
    }
    
    String gameOptionsAsText() {
        StringBuilder s = new StringBuilder();
        for (String key : gameData.getGameOptions().keySet()) {
            s.append("Option "+key+"="+gameData.getGameOptions().get(key)+ "\n");
        }
        return s.toString();
    }
    
    String playerNamesAsText() {
        StringBuilder s = new StringBuilder();
        int i=1;
        for (String player : gameData.getPlayers()) {
            s.append("Player "+(i++)+": "+player + "\n");
        }
        return s.toString();
    }
    
}
