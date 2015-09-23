package rails.game.action;

import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;

// This class does not need to be serialized
// TODO: This will have to change as soon as actions are used for online play

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class GameAction extends PossibleAction {
    
    private static final long serialVersionUID = 1L;

    public static enum Mode { SAVE, LOAD, UNDO, FORCED_UNDO, REDO, EXPORT, RELOAD }

    // Server-side settings
    protected Mode mode = null;

    // Client-side settings
    protected String filePath = null; // Only applies to SAVE, LOAD and RELOAD
    protected int moveStackIndex = -1; // target moveStackIndex, only for FORCED_UNDO and REDO

    public GameAction(Mode mode) {
        super(null); // not defined by an activity yet
        this.mode = mode;
    }

    public void setFilepath(String filepath) {
        this.filePath = filepath;
    }

    public String getFilepath() {
        return filePath;
    }

    public void setmoveStackIndex(int moveStackIndex) {
        this.moveStackIndex = moveStackIndex;
    }

    public int getmoveStackIndex() {
        return moveStackIndex;
    }
    
    public Mode getMode() {
        return mode;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        GameAction action = (GameAction)pa; 
        boolean options = Objects.equal(this.mode, action.mode);
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.filePath, action.filePath)
                && Objects.equal(this.moveStackIndex, action.moveStackIndex)
        ;
    }

    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("mode", mode)
                    .addToStringOnlyActed("filePath", filePath)
                    .addToStringOnlyActed("moveStackIndex", moveStackIndex)
                    .toString()
        ;
    }
}
