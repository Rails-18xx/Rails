package rails.game.state;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class ChangeStack {
    protected static Logger log =
        LoggerFactory.getLogger(ChangeStack.class.getPackage().getName());

    // static fields
    private final LinkedList<ChangeSet> undoStack = new LinkedList<ChangeSet>();
    private final LinkedList<ChangeSet> redoStack = new LinkedList<ChangeSet>();
    private final StateManager stateManager;
    
    // dynamic field
    private ChangeSet currentSet;

    private ChangeStack(StateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    public static ChangeStack create(StateManager stateManager) {
        ChangeStack changeStack = new ChangeStack(stateManager);
        changeStack.startGameChangeSet(true); // first set is terminal
        return changeStack;
    }
    
    /**
     * @return the current changeSet
     */
    public ChangeSet getCurrentChangeSet() {
        return currentSet;
    }

    /**
     * @return the latest (closed) changeSet
     */
    public ChangeSet getLastClosedChangeSet() {
        return undoStack.peekFirst();
    }

    private boolean closeCurrentChangeSetOnly() {
        // closes non-blocking, initial or non-empty sets
        if (currentSet.isBlocking() || currentSet.isInitial() || !currentSet.isEmpty()) {
            currentSet.close();
            // update the observers (direct and indirect)
            stateManager.updateObservers(currentSet.getObservableStates());
            undoStack.addFirst(currentSet);
            return true;
        }
        return false;
    }

    private void nextChangeSet(ChangeSet next)  {
        currentSet = next;
        log.debug(">>> Start new ChangeSet " + next + " at index=" + undoStack.size() + " <<<");
    }

    // create a new Game ChangeSet
    private void startGameChangeSet(boolean initial) {
        ChangeSet changeSet = new ChangeSet(false, initial);
        nextChangeSet(changeSet);
    }
    
    /**
     * closes the current changeSet (without providing an own)
     * this automatically creates a new (non-blocking) Game ChangeSet
     * @return the new current ChangeSet (thus the automatically created Game ChangeSet)
     */
    public ChangeSet closeCurrentChangeSet() {
        if (closeCurrentChangeSetOnly()) {
            startGameChangeSet(false);
        }
        return currentSet;
    }

    
    /**
     * Starts new ChangeSet (and closes previously open changeSet)
     * @param next the new ChangeSet to use now
     * @return the current ChangeSet (thus returns the parameter)
     */
    public ChangeSet startChangeSet(ChangeSet next) {
        // close previous set
        closeCurrentChangeSetOnly();
        // and set the next
        nextChangeSet(next);
        return currentSet;
    }
    
    /**
     * Undo command goes until an blocking ChangeSet is undone 
     * Remark: This automatically closes the current ChangeSet
     * @return the (Game) changeSet that is opened automatically
     */
    public ChangeSet undo() {
        closeCurrentChangeSetOnly();

        // keep track of all states (for update)
        ImmutableSet.Builder<State> statesToUpdate = ImmutableSet.builder();

        while (true) {
            // otherwise unexecute and move from undoStack add to redoStack
            ChangeSet undoSet = undoStack.peekFirst();
            undoSet.unexecute(); // throws IllegalStateException if not possible
            statesToUpdate.addAll(undoSet.getObservableStates());
            undoStack.removeFirst();
            redoStack.addFirst(undoSet);
            // stop if blocking set is reached 
            if (undoSet.isBlocking()) break;
        }
        // update observers of the states undone
        stateManager.updateObservers(statesToUpdate.build());

        startGameChangeSet(false);
        return currentSet;
    }
    
    /**
     * Redo command goes until (another) blocking ChangeSet gets into sight or the redo stack is empty).
     * Remark: This automatically closes the current ChangeSet
     * If the redo stack is empty, nothing happens.
     * @return the current ChangeSet (potentially an automatically opened game ChangeSet)
     */
    public ChangeSet redo() {
        closeCurrentChangeSetOnly();
        if (redoStack.size() == 0) return currentSet;
        
        // keep track of all states (for update)
        ImmutableSet.Builder<State> statesToUpdate = ImmutableSet.builder();
        
        while (true) { 
            // the first set is always the action changeSet
            ChangeSet redoSet = redoStack.removeFirst();
            redoSet.reexecute();
            // add states for update
            statesToUpdate.addAll(redoSet.getObservableStates());
            undoStack.addFirst(redoSet);
            // break if stack is empty the next blocking ChangeSet is in view
            if (redoStack.size() == 0 || redoStack.peekFirst().isBlocking()) break;
        }
        // update observers of the states redone
        stateManager.updateObservers(statesToUpdate.build());
        
        // start a new game changeSet 
        startGameChangeSet(false);
        
        return currentSet;
    }
    
    /**
     * @return size of UndoStack (including game ChangeSets)
     */
    public int sizeUndoStack() {
        return undoStack.size();
    }
    
    /**
     * @return size of RedoStack (including game ChangeSets)
     */
    public int sizeRedoStack() {
        return redoStack.size();
    }
    
}
