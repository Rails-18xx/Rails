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
 * In general activities can be 
 * - User triggered: Thus they belong to an action of a player.
 * - Game triggered: Changes that are mainly triggered by an automated game action (e.g. a scoring phase, end of turn 
 * or round activities)
 * 
 * ChangeSet objects are stored in the ChangeStack.
 */

public class ChangeSet {

    protected static Logger log =
        LoggerFactory.getLogger(ChangeSet.class.getPackage().getName());

    // static fields
    private final List<Change> changes = Lists.newArrayList();
    private final boolean blocking;
    private final boolean initial;
    
    // dynamic fields
    private boolean closed = false; 
    
    protected ChangeSet(boolean blocking, boolean initial) {
        this.blocking = blocking;
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
        // immediate execution
        change.execute();
        change.getState().updateModels();
        log.debug("Added change: " + change);
    }
    
    /**
     * closes ChangeSet: No further Changes can be added, undo and redo is possible
     */
    void close() {
        checkState(!closed, "ChangeSet is closed already");
        closed = true;
    }
    
   /**
    * retrieves all (observable) states that are changed by Changes in the ChangeSet
    * @return set of all states affected by Changes
    */
   ImmutableSet<State> getObservableStates() {
        ImmutableSet.Builder<State> builder = new ImmutableSet.Builder<State>();
        for (Change change:changes) {
            if (change.getState().isObservable()){
                builder.add(change.getState());
            }
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
    
    boolean isInitial() {
        return initial;
    }
    
    boolean isBlocking() {
        return blocking;
    }

}
