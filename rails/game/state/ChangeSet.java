package rails.game.state;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import rails.game.Player;

/**
 * A ChangeSet object represents the collection of all changes
 * that belong to one joint action.
 * 
 *  ChangeSet objects are stored in the ChangeStack.
 *  
 * @author freystef
 */

final class ChangeSet {

    protected static Logger log =
        Logger.getLogger(ChangeSet.class.getPackage().getName());

    private final Player owner;

    private final List<Change> changes = new ArrayList<Change>();
    
    private boolean closed; 
    
    private ImmutableSet<State> states;

    ChangeSet(Player owner) {
        this.owner = owner;
        closed = false;
    }

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

    boolean isUndoableByPlayer (Player player) {
        return owner.equals(player);
    }
    
    void updateStates() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        
        for (State state:states) {
            state.notifyModel();
        }
    }
    
    ImmutableSet<State> getStates() {
        return states;
    }
    
}
