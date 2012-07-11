package rails.game.state;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * A ChangeSet object represents the collection of all changes
 * that belong to the same action.
 * 
 * This can be either a player action (ActionChangeSet) or a game induced automatic
 * (AutoChangeSet) action. 
 * 
 * ChangeSet objects are stored in the ChangeStack.
 *  
 * @author freystef
 */

abstract class ChangeSet {

    protected static Logger log =
        LoggerFactory.getLogger(ChangeSet.class.getPackage().getName());

    private final List<Change> changes = new ArrayList<Change>();
    
    private boolean closed = false; 
    
    private ImmutableSet<State> states = null;

    // constructor
    protected ChangeSet() {};

    /**
     * adds change to the ChangeSet and executes the change
     * @param change
     */
    void addChange (Change change) {
        if (closed) throw new IllegalStateException("ChangeSet is closed");
        changes.add(change);
        // immediate execution
        change.execute();
        change.getState().updateModels();
        log.debug("Added change: " + change);
    }

    void close() {
        if (closed) throw new IllegalStateException("ChangeSet is already closed");
        // define all states added
        defineStates();
        closed = true;
    }
    
    private void defineStates() {
        ImmutableSet.Builder<State> builder = new ImmutableSet.Builder<State>();
        for (Change change:changes) {
            if (change.getState().isObservable()){
                builder.add(change.getState());
            }
        }
        states = builder.build();
    }
    
    void reexecute() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        for (Change change:changes) {            
            change.execute();
            change.getState().updateModels();
            log.debug("Redo: " + change);
        }
    }

    void unexecute() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        // iterate reverse
        for (Change change:Lists.reverse(changes)) {
            change.undo();
            change.getState().updateModels();
            log.debug("Undone: " + change);
        }
    }

    boolean isClosed() {
        return closed;
    }
    
    boolean isEmpty() {
        return changes.isEmpty();
    }
    
    abstract boolean isTerminal();

    /**
     * @return set of States in the ChangeSet
     * Remark: It only includes those which are observable (isObservable = true)
     */
    ImmutableSet<State> getStates() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        return states;
    }
    
    

    
}
