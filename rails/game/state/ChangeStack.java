package rails.game.state;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import rails.game.Player;
import rails.game.ReportBuffer;
import rails.game.action.PossibleAction;

public final class ChangeStack {
    protected static Logger log =
        Logger.getLogger(ChangeStack.class.getPackage().getName());

    private final LinkedList<ChangeSet> stack = new LinkedList<ChangeSet>();
    private boolean enabled = false;

    private ChangeStack() {}
    
    static ChangeStack create() {
        return new ChangeStack();
    }
    
    /**
     * Start making moves undoable. Will be called once, after all
     * initialisations are complete.
     */
    public void enable() {
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    private void checkEnabled() {
        if (!enabled) throw new IllegalStateException("ChangeStack is not enabled");
    }

    /**
     * Returns a valid ChangeSet that is current for the ChangeStack
     * If the ChangeStack is not enabled or empty a IllegalStateExcpetion is thrown
     * @return the current changeSet
     */
    public ChangeSet getAvailableChangeSet() {
        // check preconditions
        checkEnabled();
        if (stack.isEmpty()) throw new IllegalStateException("No ChangeSet on ChangeStack");
        
        // return the last on the 
        return stack.peekLast();
    }
    
    /**
     * Returns a valid ChangSset that is current for the ChangeStack
     * If the ChangeStack is not enabled or empty a IllegalStateExcpetion is thrown
     * If the ChangeSet does not meet the conditions of the expectOpen argument a IllegalStateException is thrown
     * @param expectOpen if yes 
     * @return the current changeSet
     */
    public ChangeSet getAvailableChangeSet(boolean expectOpen) {
        // check preconditions by using the private 
        ChangeSet changeSet = getAvailableChangeSet();

        if (expectOpen) {
            if (changeSet.isClosed()) throw new IllegalStateException("Current ChangeSet is closed already");
        } else {
            if (!changeSet.isClosed()) throw new IllegalStateException("Current ChangeSet not closed yet");
        }
        return changeSet;
    }
    
    /**
     * Creates new ActionChangeSet 
     * @param activePlayer
     * @param executedAction
     * @return the new current ChangeSet
     */
    public ActionChangeSet start(Player player, PossibleAction action) {
        
        // check preconditions
        checkEnabled();
       
        if (stack.peekLast() != null && !stack.peekLast().isClosed()) 
            throw new IllegalStateException("Current ChangeSet not closed yet");
        
        // create new ChangeSet
        ActionChangeSet changeSet = new ActionChangeSet(player, action);
        
        stack.offerLast(changeSet);
        log.debug(">>> Start ChangeSet " + changeSet + " at index=" + stack.size() + " <<<");

        // TODO: Check if this is the correct place to create the report Item
        ReportBuffer.createNewReportItem(getCurrentIndex());
        
        return changeSet;
    }

    
    // TODO: Write that implementation
    private void updateObservers() {
        
//        StateManager.getInstance().updateObservers();
        
    }
    
    /**
     * Finish and closes current ChangeSet
     */
    public void finish() {
        // retrieve closed changeSet
        ChangeSet changeSet = getAvailableChangeSet(false);

        // close ChangeSet
        if (changeSet.isEmpty()) {
            // discard empty set
            stack.remove(changeSet);
            log.warn("ChangeSet to finish is empty and will be discarded");
        } else {
            changeSet.close();
            updateObservers();
        }
    }

    /**
     * Cancels current ChangeSet (if not closed yet)
    */
    public void cancel() {
        // retrieve open changeSet
        ChangeSet changeSet = getAvailableChangeSet(true);

        // un-execute, update and remove
        changeSet.unexecute();
        
        stack.removeLast();
        // TODO: Check if this is really needed
        // updateObservers
    }

    /**
     * Add change to the current ChangeSet
     */
    public void addChange (Change change) {
        // retrieve open changeSet and add change
        getAvailableChangeSet(true).addChange(change);
    }


    public boolean isUndoableByPlayer(Player player) {
        return isUndoableByManager() && getAvailableChangeSet().isUndoableByPlayer(player);
    }

    public boolean isUndoableByManager() {
        return enabled && stack.size() != 0;
    }

    public boolean isOpen() {
        return enabled && stack.size() != 0 && !getAvailableChangeSet().isClosed();
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
    @Deprecated
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
    @Deprecated
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

    public boolean isRedoable() {
        // TODO : Write this method
        return false;
    }


    public void redoMoveSet() {
        // TODO: Write this method
        
    }

    public void undo(boolean b) {
        
        
    }

    /* Static methods to enable access from anywhere */
    static void add (Change change) {
        change.execute();
        
        // get changeStack via StateManager
        change.getState().getStateManager().getChangeStack().addChange(change);
    }

    
}
