package rails.common.parser;

import java.util.ArrayList;

import rails.common.GameOption;

/**
 * GameInfo holds basic information about the game, such as:
 * 1. List of available game names.
 * 2. Min. and Max. players for the game.
 * 3. Available game options
 * 4. Game credits.
 */
public class GameInfo {

    private boolean complete;
    
    private int minPlayers;
    private int maxPlayers;

    private String name;
    private String note;
    private String description;
    
    private ArrayList<GameOption> options;
    
    public GameInfo() {
        // Initialize with bogus strings. These will be overwritten when the XML is read.
        name = "Player";
        note = "Notes";
        description = "Description";
        
        minPlayers = -1;
        maxPlayers = -1;
        
        options = new ArrayList<GameOption>();
        complete = false;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<GameOption> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<GameOption> options) {
        this.options = options;
    }
}
