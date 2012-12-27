package rails.game.state;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ChangeStack {

    protected static Logger log =
        LoggerFactory.getLogger(ChangeStack.class);

    // static fields
    private final StateManager stateManager;

    private final List<ChangeSet> undoStack = Lists.newArrayList();
    private final List<ChangeSet> redoStack = Lists.newArrayList();
    private final ChangeReporter reporter;
    
    // dynamic fields
    private ImmutableList.Builder<Change> changeBuilder = ImmutableList.builder();

    private ChangeStack(StateManager stateManager, ChangeReporter reporter) {
        this.stateManager = stateManager;
        this.reporter = reporter;
    }
    
    /**
     * Creates a new ChangeStack with an initial ChangeSet
     */
    public static ChangeStack create(StateManager stateManager, ChangeReporter changeReporter) {
        ChangeStack changeStack = new ChangeStack(stateManager, changeReporter);
        changeReporter.setChangeStack(changeStack);
        return changeStack;
    }
    
    /**
     * @return the previous (closed) changeSet
     */
    public ChangeSet getPreviousChangeSet() {
        checkState(getCurrentIndex() >= 0, "No ChangeSet available at start");
        return undoStack.get(getCurrentIndex());
    }
    
    /**
     * Add change to current changeSet
     */
    void addChange(Change change) {
        log.debug("ChangeSet: Add " + change);
        changeBuilder.add(change);
        // immediate execution and information of models
        change.execute();
        change.getState().informTriggers(change);
    }
    
    
    void addMessage(String message) {
        log.info("Message: " + message);
        reporter.addMessage(message);
    }
    
    private boolean checkRequirements(ChangeAction action) {
        if (changeBuilder.build().isEmpty() || action == null) {
            return false;
        } else {
            return true;
        }
    }
    
    public void close(ChangeAction action) {
        if (checkRequirements(action)) {
            ChangeSet set = new ChangeSet(changeBuilder.build(), action, getCurrentIndex());
            reporter.close(set);
            log.debug("<<< Closed changeSet " + set + " at index =" + getCurrentIndex());
            undoStack.add(set);
            redoStack.clear();
            // restart builders
            restart();
            // inform direct and indirect observers
            updateObservers(set.getStates());
        }
    }

    private void restart() {
        changeBuilder = ImmutableList.builder();
    }
    
    
    public void updateObservers(Set<State> states) {
        // update the observers of states and models
        log.debug("ChangeStack: update Observers");
        stateManager.updateObservers(states);
        reporter.update();
    }
    
    // undo return 
    public boolean isUndoPossible() {
        return (getCurrentIndex() >= 1);
    }

    public boolean isUndoPossible(ChangeActionOwner owner) {
        return (isUndoPossible() && 
                undoStack.get(getCurrentIndex()-1).getOwner() == owner);
    }
    
    /**
     * Undo command
     */
    public void undo() {
        checkState(getCurrentIndex() > 1, "Undo not possible");
        ChangeSet undoSet = executeUndo();
        restart();
        updateObservers(undoSet.getStates());
    }
    
    /**
     * Example: Undo-Stack has 4 elements (0,1,2,3), current-Index = 4
     * Undo to index 2, requires removing the latest element, such that current-Index = 3
     */
    
    public void undo(int index) {
        checkState(index < getCurrentIndex() && index >= 1, "Undo not possible");
        ImmutableSet.Builder<State> states = ImmutableSet.builder();
        while (getCurrentIndex() > index) {
            states.addAll(executeUndo().getStates());
        }
        restart();
        updateObservers(states.build());
    }

    private ChangeSet executeUndo() {
        log.debug("Size of undoStack = " + undoStack.size());
        ChangeSet undoSet = undoStack.remove(getCurrentIndex()-1);
        log.debug("UndoSet = " + undoSet);
        undoSet.unexecute();
        redoStack.add(undoSet);
        return undoSet;
    }
    
    
    public boolean isRedoPossible() {
        return (!redoStack.isEmpty());
    }

    public boolean isRedoPossible(ChangeActionOwner owner) {
        return (isRedoPossible() && 
                redoStack.get(redoStack.size() - 1).getOwner() == owner);
    }
    
    /**
     * Redo command
     * @throws IllegalStateException if redo stack is empty or there is an open ChangeSet
     */
    public void redo() {
        checkState(!redoStack.isEmpty(), "Redo not possible");
        
        ChangeSet redoSet = executeRedo();
        restart();
        updateObservers(redoSet.getStates());
    }

    public void redo(int index) {
        checkState(!redoStack.isEmpty() && index <= getMaximumIndex(), 
                "Redo not possible");

        ImmutableSet.Builder<State> states = ImmutableSet.builder();
        while (getCurrentIndex() < index) {
            states.addAll(executeRedo().getStates());
        }
        restart();
        updateObservers(states.build());
    }
    
    private ChangeSet executeRedo() {
        ChangeSet redoSet = redoStack.remove(redoStack.size() - 1);
        redoSet.reexecute();
        undoStack.add(redoSet);
        
        return redoSet;
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
    public int getMaximumIndex() {
        return redoStack.size() + getCurrentIndex();
    }

}
