package rails.game.state;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * A ChangeSet object represents the collection of all changes
 * that belong to the same activity.
 * 
 * ChangeSet objects are stored in the ChangeStack.
 */

public class ChangeSet {

    private static final Logger log = LoggerFactory.getLogger(ChangeSet.class);

    // static fields
    private final ChangeAction action;
    private final List<Change> changes = Lists.newArrayList();
    private final boolean initial;
    
    // dynamic fields
    private boolean closed = false; 
    
    protected ChangeSet(ChangeAction action, boolean initial) {
        this.action = action;
        this.initial = initial;
    };

    /**
     * adds change to the ChangeSet and executes the change (including updates of the depending models)
     * @param change to add to the ChangeSet
     * @throws IllegalStateException if ChangeSet is closed
     */
    void addChange (Change change) {
        checkState(!closed, "ChangeSet is closed");
        changes.add(change);
        // immediate execution and update of models
        change.execute();
        change.getState().updateModels();
        log.debug("Add " + change);
    }
    
    /**
     * closes ChangeSet: No further Changes can be added, undo and redo is possible
     */
    void close() {
        checkState(!closed, "ChangeSet is closed already");
        closed = true;
    }
    
   /**
    * retrieves all states that are changed by Changes in the ChangeSet
    * @return set of all states affected by Changes
    */
   ImmutableSet<State> getStates() {
        ImmutableSet.Builder<State> builder = new ImmutableSet.Builder<State>();
        for (Change change:changes) {
            builder.add(change.getState());
        }
        return builder.build();
    }
    
   /**
    * re-execute all Changes in the ChangeSet (redo)
    * @ŧhrows IllegalStateException if ChangeSet is still open 
    */
   void reexecute() {
        checkState(closed, "ChangeSet is still open");
        for (Change change:changes) {            
            change.execute();
            change.getState().updateModels();
            log.debug("Redo: " + change);
        }
    }
   
   /**
    * un-executed all Changes in the ChangeSet (undo)
    * @throws IllegalStateException if ChangeSet is still open or ChangeSet is initial
    */
    void unexecute() {
        if (!closed) throw new IllegalStateException("ChangeSet is still open");
        if (initial) throw new IllegalStateException("ChangeSet is initial - cannot be undone");
        
        // iterate reverse
        for (Change change:Lists.reverse(changes)) {
            change.undo();
            change.getState().updateModels();
            log.debug("Undone: " + change);
        }
    }

    /**
     * returns the ChangeSet open/close state
     * @return true if ChangeSet is closed
     */
    boolean isClosed() {
        return closed;
    }
    
    /**
     * checks if the ChangeSet is empty
     * @return true if ChangeSet is empty
     */
    boolean isEmpty() {
        return changes.isEmpty();
    }
    
    /**
     * returns the ChangeAction associated with the ChangeSet
     * @return the associated ChangeAction
     */
    ChangeAction getAction() {
        return action;
    }

    /**
     * returns true if the ChangeSet is an initial state
     * @return true if initial
     */
    boolean isInitial() {
        return initial;
    }
}
