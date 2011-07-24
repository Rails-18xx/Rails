package rails.game.state;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.GameManagerI;
import rails.game.Player;
import rails.game.ReportBuffer;

public final class ChangeStack {
    protected static Logger log =
        Logger.getLogger(ChangeStack.class.getPackage().getName());

    private final LinkedList<ChangeSet> stack = new LinkedList<ChangeSet>();

    private boolean enabled = false;

    public ChangeStack () {
    }

    /**
     * Start making moves undoable. Will be called once, after all
     * initialisations are complete.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * returns active ChangeSet
     * @param activePlayer
     * @return new ChangeSet
     */
    
    public ChangeSet start(Player activePlayer) {
        
        // check preconditions
        if (!enabled) throw new IllegalStateException("ChangeStack is not enabled");
        if (stack.peekLast() != null && !stack.peekLast().isClosed()) throw new IllegalStateException("Current ChangeSet not closed yet");
        
        // create new ChangeSet
        ChangeSet changeSet = new ChangeSet(activePlayer);
        stack.offerLast(changeSet);
        log.debug(">>> Start ChangeSet(index=" + stack.size() + ")");

        // TODO: Check if this is the correct place to create the report Item
        ReportBuffer.createNewReportItem(getCurrentIndex());
        
        return changeSet;
    }

    private ChangeSet getCurrentChangeSet() {
        // check preconditions
        if (!enabled) throw new IllegalStateException("ChangeStack is not enabled");
        ChangeSet changeSet = stack.peekLast();
        if (changeSet == null) throw new IllegalStateException("No ChangeSet on ChangeStack");
        return changeSet;
    }
    
    private ChangeSet getCurrentChangeSet(boolean expectOpen) {
        // check preconditions
        ChangeSet changeSet = getCurrentChangeSet();
        if (expectOpen) {
            if (changeSet.isClosed()) throw new IllegalStateException("Current ChangeSet is closed already");
        } else {
            if (!changeSet.isClosed()) throw new IllegalStateException("Current ChangeSet not closed yet");
        }
        return changeSet;
    }
    
    
    /**
     * Finish and closes current ChangeSet
     */
    
    public void finish() {
        // retrieve closed changeSet
        ChangeSet changeSet = getCurrentChangeSet(false);

        // close ChangeSet
        if (changeSet.isEmpty()) {
            // discard empty set
            stack.removeLast();
            log.warn("Action to finish is empty and will be discarded");
        } else {
            changeSet.close();
            changeSet.updateStates();
        }
    }

    /**
     * Cancels current ChangeSet (if not closed yet)
    */
    public void cancel() {
        // retrieve open changeSet
        ChangeSet changeSet = getCurrentChangeSet(true);

        // un-execute, update and remove
        changeSet.unexecute();
        changeSet.updateStates();
        stack.removeLast();
    }

    /**
     * Add change to the current ChangeSet
     */
    public void addChange (Change change) {
        // retrieve open changeSet and add change
        getCurrentChangeSet(true).addChange(change);
    }


    public boolean isUndoableByPlayer(Player player) {
        return isUndoableByManager() && getCurrentChangeSet().isUndoableByPlayer(player);
    }

    public boolean isUndoableByManager() {
        return enabled && stack.size() != 0;
    }

    public boolean isOpen() {
        return enabled && stack.size() != 0 && !getCurrentChangeSet().isClosed();
    }
    
    // TODO: What is correct?
    public int getIndex() {
        return stack.size();
    }

    // TODO: What is correct
    public int size() {
        return stack.size();
    }
    
    /** 
     * the current index is the one of either the open moveset or
     * if none is open of the latest added
     */
    public int getCurrentIndex() {
        if (isOpen()) {
            return getIndex() + 1;
        } else {
            return getIndex();
        }
    }
    
    /**
     * undo/redo to a given moveStack index
     * TODO: the redo-part has to be replaced by execution actions
     */
    public boolean gotoIndex(int index) {
/*        if (getIndex() == index) return true;
        else if (getIndex() > index) {
            while (getIndex() < index) {
                if (!redoMoveSet()) return false;
            }
        } else {
            while (getIndex() > index) {
                if (!undoMoveSet(true)) return false;
            }
        }; */
        return true;
    }

    /* Static methods to enable access from anywhere */
    static void add (Change change) {
        change.execute();
        
        ChangeStack changeStack = getChangeStack();
        if (changeStack != null) changeStack.addChange(change);
    }

    private static ChangeStack getChangeStack() {
        GameManagerI gameManager = GameManager.getInstance();
        if (gameManager != null) {
            return gameManager.getChangeStack();
        } else {
            // No GM during game setup; no problem, as MoveSets are not yet enabled then.
            return null;
        }
    }

    public boolean isRedoable() {
        // TODO : Write this method
        return false;
    }

    public void linkToPreviousMoveSet() {
        // TODO: Remove all calls to this method
        
    }

    public void start(boolean b) {
        // TODO: Remove all calls to this method
        
    }

    public void redoMoveSet() {
        // TODO: Write this method
        
    }

    public void undo(boolean b) {
        // TODO: Write this method
        
    }

}
