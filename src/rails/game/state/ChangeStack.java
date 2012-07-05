package rails.game.state;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.Player;
import rails.game.action.PossibleAction;

// TODO: ReportBuffer addition
// FIXME: Undo and other mechanisms

public class ChangeStack {
    protected static Logger log =
        LoggerFactory.getLogger(ChangeStack.class.getPackage().getName());

    private final Deque<ChangeSet> undoStack = new ArrayDeque<ChangeSet>();
    private final Deque<ChangeSet> redoStack = new ArrayDeque<ChangeSet>();

    private ChangeStack() {};
    
    public static ChangeStack create() {
        ChangeStack changeStack = new ChangeStack();
        changeStack.startAutoChangeSet();
        return changeStack;
    }
    
    /**
     * @return the current changeSet
     */
    public ChangeSet getCurrentChangeSet() {
        return undoStack.peekFirst();
    }
    
    /**
     * closes the current changeSet
     * empty ActionChangeSets get removed instead
     */
    public void closeCurrentChangeSet() {
        ChangeSet changeSet = getCurrentChangeSet();
        // check if already closed
        if (changeSet.isClosed()) return;
        // remove empty AutoChangeSet
        if (changeSet instanceof AutoChangeSet && changeSet.isEmpty()) {
            undoStack.removeFirst();
        }
        // otherwise close the changeSet
        changeSet.close();
    }
    
    /**
     * Creates a new AutoChangeSet
     * @return new AutoChangeSet
     */
    public AutoChangeSet startAutoChangeSet() {
        AutoChangeSet changeSet = new AutoChangeSet();
        undoStack.addFirst(changeSet);
        log.debug(">>> Start AutoChangeSet " + changeSet + " at index=" + undoStack.size() + " <<<");

        return changeSet;
    }
    
    /**
     * Creates new ActionChangeSet
     * @param player the owning player of the action
     * @param action the action that is connected
     * @return new ActionChangeSet
     */
    public ActionChangeSet startActionChangeSet(Player player, PossibleAction action) {
        ActionChangeSet changeSet = new ActionChangeSet(player, action);
        undoStack.addFirst(changeSet);
        log.debug(">>> Start ActionChangeSet " + changeSet + " at index=" + undoStack.size() + " <<<");

        // TODO: Check if this is the correct place to create the report Item
        // ReportBuffer.createNewReportItem(getCurrentIndex());
        return changeSet;
    }
    
    /**
     * Undo command
     */
    public void undo() {
        ChangeSet undoSet = undoStack.pollFirst();
        while (undoSet != null) { 
            undoSet.unexecute();
            redoStack.addFirst(undoSet);
            if (undoSet instanceof ActionChangeSet) break;
            undoSet = undoStack.pollFirst();
        }
        startAutoChangeSet();
    }
    
    /**
     * Redo command
     */
    public void redo() {
        ChangeSet redoSet = redoStack.pollFirst();
        while (redoSet != null) { 
            redoSet.unexecute();
            undoStack.addFirst(redoSet);
            if (redoSet instanceof ActionChangeSet) break;
            redoSet = redoStack.pollFirst();
        }
        startAutoChangeSet();
    }
}
