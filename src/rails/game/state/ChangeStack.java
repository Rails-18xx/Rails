package rails.game.state;

import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeStack {
    protected static Logger log =
        LoggerFactory.getLogger(ChangeStack.class);

    // static fields
    private final LinkedList<ChangeSet> undoStack = new LinkedList<ChangeSet>();
    private final LinkedList<ChangeSet> redoStack = new LinkedList<ChangeSet>();
    private final StateManager stateManager;
    
    // dynamic field
    private ChangeSet currentSet;

    private ChangeStack(StateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    /**
     * Creates a new ChangeStack with an initial ChangeSet
     */
    public static ChangeStack create(StateManager stateManager) {
        ChangeStack changeStack = new ChangeStack(stateManager);
        changeStack.startChangeSet(null, true);
        return changeStack;
    }
    
    /**
     * @return the current (open) changeSet
     */
    public ChangeSet getCurrentChangeSet() {
        return currentSet;
    }
    
    /**
     * @return the previous (closed) changeSet, null if empty
     */
    public ChangeSet getPreviousChangeSet() {
        return undoStack.peekFirst();
    }
    
    /**
     * @return true if there is an current (open) changeSet available
     */
    public boolean isOpen() {
        // the latter condition should always be true, but this could change
        return currentSet != null && !currentSet.isClosed();
    }

    /**
     * Add change to current changeSet
     * @throws IllegalStateException if current changeSet is null
     */
    void addChange(Change change) {
        checkState(currentSet != null, "No open ChangeSet to add change to");
        currentSet.addChange(change);
    }
    
    /**
     * closes the current changeSet and moves it to the undoStack
     * sets the currentSet to null
     * @return the closed changeSet
     * @throws IllegalStateException if current changeSet is null 
     */
    public ChangeSet close() {
        checkState(currentSet != null, "No open ChangeSet to close");
        
        // close and store for return
        currentSet.close();
        ChangeSet storeCurrentSet = currentSet;
        log.debug("<<< Closed changeSet " + currentSet + " at index =" + getCurrentIndex());
        
        // update the observers (direct and indirect)
        stateManager.updateObservers(currentSet.getStates());
        undoStack.addFirst(currentSet);
        
        // set null and return stored
        currentSet = null;
        return storeCurrentSet;
    }
    
    /**
     * Cancels the current open set
     */
    public void cancel() {
        currentSet = null;
    }
   
    /**
     * Starts new ChangeSet (closes if one is still open)
     * @param action associated ChangeAction
     * @return the new current ChangeSet
     */
    public ChangeSet newChangeSet(ChangeAction action) {
        if (isOpen()) close();
        return startChangeSet(action, false);
    }

    private ChangeSet startChangeSet(ChangeAction action, boolean initial) {
        checkState(currentSet == null, "An unclosed ChangeSet still open");

        currentSet = new ChangeSet(action, initial);
        log.debug(">>> Start new ChangeSet " + currentSet + " at index=" + getCurrentIndex());
        return currentSet;
    }
    
    /**
     * Undo command
     * @throws IllegalStateException if remaining ChangeSet is initial or there is an open ChangeSet
     */
    public void undo() {
        checkState(currentSet == null, "An unclosed ChangeSet still open");

        ChangeSet undoSet = undoStack.peekFirst();
        checkState(!undoSet.isInitial(), "ChangeSet with intial attribute on top of undo stack");
        
        undoStack.pollFirst();
        undoSet.unexecute();
        redoStack.addFirst(undoSet);
        stateManager.updateObservers(undoSet.getStates());
    }
    
    /**
     * Redo command
     * @throws IllegalStateException if redo stack is empty or there is an open ChangeSet
     */
    public void redo() {
        checkState(currentSet == null, "An unclosed ChangeSet still open");
        checkState(!redoStack.isEmpty(), "RedoStack is empty");
        
        ChangeSet redoSet = redoStack.pollFirst();
        redoSet.reexecute();
        undoStack.addFirst(redoSet);
        stateManager.updateObservers(redoSet.getStates());
    }
    
    /**
     * @return current index of the ChangeStack
     */
    public int getCurrentIndex() {
        return undoStack.size();
    }
    
    /**
     * @return size of RedoStack
     */
    public int getSizeRedoStack() {
        return redoStack.size();
    }
    
}
