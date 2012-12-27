package rails.util;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import rails.game.action.PossibleAction;

/**
 * Combines all fields required for game IO
 * Defines the complete game
 * 
 * TODO: Rewrite this from scratch
 * */
public class GameData {
    class MetaData {
        String version;
        String date;
        Long fileVersionID;
        String gameName;
    };
    
    MetaData meta = new MetaData();
    Map<String, String> gameOptions;
    List<String> playerNames;
    List<PossibleAction> actions;
    SortedMap<Integer, String> userComments;
    
    String metaDataAsText() {
        StringBuilder s = new StringBuilder();
        s.append("Rails saveVersion = " + meta.version + "\n");
        s.append("File was saved at " + meta.date + "\n");
        s.append("Saved versionID=" + meta.fileVersionID + "\n");
        s.append("Save game=" + meta.gameName + "\n");
        return s.toString();
    }
    
    String gameOptionsAsText() {
        StringBuilder s = new StringBuilder();
        for (String key : gameOptions.keySet()) {
            s.append("Option "+key+"="+gameOptions.get(key)+ "\n");
        }
        return s.toString();
    }
    String playerNamesAsText() {
        StringBuilder s = new StringBuilder();
        int i=1;
        for (String player : playerNames) {
            s.append("Player "+(i++)+": "+player + "\n");
        }
        return s.toString();
    }

    
}
