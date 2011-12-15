package rails.game.state;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.Player;

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
        Logger.getLogger(ChangeSet.class.getPackage().getName());

    private final List<Change> changes = new ArrayList<Change>();
    
    private boolean closed = false; 
    
    private ImmutableSet<State> states = null;

    // uses default constructor

    /**
     * adds change to the ChangeSet and executes the change
     * @param change
     */
    
    void addChange (Change change) {
        if (closed) throw new IllegalStateException("ChangeSet is closed");
        changes.add(change);
        // immediate execution
        change.execute();
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
            builder.add(change.getState());
        }
        states = builder.build();
    }
    
    void reexecute() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        for (Change change:changes) {            
            change.execute();
            log.debug("Redo: " + change);
        }
    }

    void unexecute() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        // iterate reverse
        for (Change change:Lists.reverse(changes)) {
            change.undo();
            log.debug("Undone: " + change);
        }
    }

    boolean isClosed() {
        return closed;
    }
    
    boolean isEmpty() {
        return changes.isEmpty();
    }

    @Deprecated
    abstract boolean isUndoableByPlayer(Player player);
    
    ImmutableSet<State> getStates() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        return states;
    }
    
}
