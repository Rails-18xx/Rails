package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
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
    private final List<Change> changes;
    private final ChangeAction action;
    private final int index;
    
    ChangeSet(List<Change> changes, ChangeAction action, int index) {
        this.changes = changes;
        this.action = action;
        this.index = index;
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
        for (Change change:changes) {            
            change.execute();
            log.debug("Redo: " + change);
        }
    }
   
   /**
    * un-executed all Changes in the ChangeSet (undo)
    * @throws IllegalStateException if ChangeSet is still open or ChangeSet is initial
    */
    void unexecute() {
        checkState(index != -1, "ChangeSet is initial - cannot be undone");
        
        // iterate reverse
        for (Change change:Lists.reverse(changes)) {
            log.debug("About to undo: " + change);
            change.undo();
            log.debug("Undone: " + change);
        }
    }

    /**
     * returns the ChangeAction associated with the ChangeSet
     * @return the associated ChangeAction
     */
    ChangeAction getAction() {
        return action;
    }
    
    /**
     * returns the Owner associated with the ChangeSet
     */
    public ChangeActionOwner getOwner() {
        return action.getActionOwner();
    }
    
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("action", action)
                .add("Owner", getOwner())
                .add("Index", index)
                .toString();
    }
}