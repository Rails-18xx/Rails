package rails.game.state;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import rails.game.Player;
import rails.game.action.PossibleAction;

// TODO: ReportBuffer addition
// FIXME: Undo and other mechanisms

public class ChangeStack {
    protected static Logger log =
        LoggerFactory.getLogger(ChangeStack.class.getPackage().getName());

    private final Deque<ChangeSet> undoStack = new ArrayDeque<ChangeSet>();
    private final Deque<ChangeSet> redoStack = new ArrayDeque<ChangeSet>();
    private final StateManager stateManager;

    private ChangeSet currentSet;

    private ChangeStack(StateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    public static ChangeStack create(StateManager stateManager) {
        ChangeStack changeStack = new ChangeStack(stateManager);
        changeStack.startAutoChangeSet(true); // first set is terminal
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
    
    // only closes the current change set (without opening a new one)
    // an empty ActionChangeSet gets removed
    private boolean closeCurrentChangeSetOnly() {
        // if empty non-terminal AutoChangeSet remove it
        if (currentSet instanceof AutoChangeSet && currentSet.isEmpty() && !((AutoChangeSet) currentSet).isTerminal()) {
            return false;
        } else {
            currentSet.close();
            // FIXME: This is a hack, has to replaced
            updateObservers(Sets.newHashSet(currentSet));
        }
        undoStack.addFirst(currentSet);
        return true;
    }

    // create a new AutoChangeSet
    private void startAutoChangeSet(boolean terminal) {
        AutoChangeSet changeSet = new AutoChangeSet(terminal);
        log.debug(">>> Start AutoChangeSet " + changeSet + " at index=" + undoStack.size() + " <<<");
        currentSet = changeSet;
    }
    
    /**
     * closes the current changeSet
     * @return (new) open AutoChangeSet
     */
    public AutoChangeSet closeCurrentChangeSet() {
        if (closeCurrentChangeSetOnly()) {
            startAutoChangeSet(false);
        }
        return (AutoChangeSet)currentSet;
    }

    /**
     * Creates new ActionChangeSet (and closes previously open changeSet)
     * @param player the owning player of the action
     * @param action the action that is connected
     * @return new ActionChangeSet
     */
    public ActionChangeSet startActionChangeSet(Player player, PossibleAction action) {
        // close previous set
        closeCurrentChangeSetOnly();
        
        ActionChangeSet changeSet = new ActionChangeSet(player, action);
        log.debug(">>> Start ActionChangeSet " + changeSet + " at index=" + undoStack.size() + " <<<");
        currentSet = changeSet;

        // TODO: Check if this is the correct place to create the report Item
        // ReportBuffer.createNewReportItem(getCurrentIndex());
        return changeSet;
    }
    
    
    /**
     * Undo command
     * Remark: this closes the current ChangeSet
     */
    public boolean undo() {
        // check if there is a terminal changeSet 
        // TODO: Should be replaced by a better control of undo allowed
        if (undoStack.peekFirst().isTerminal()) return false;

        // if not, close and start undoing
        closeCurrentChangeSetOnly();
        while (true) {
            // otherwise remove, unexecute and add to redoStack 
            ChangeSet undoSet = undoStack.removeFirst();
            undoSet.unexecute();
            redoStack.addFirst(undoSet);
            if (undoSet instanceof ActionChangeSet) break;
        }
        startAutoChangeSet(false);
        return true;
    }
    
    /**
     * Redo command
     * Remark: this closes the current ChangeSet
     */
    public boolean redo() {
        // check if the there are changesets on the redo stack
        // TODO: Should be replaced by a better control of redo allowed
        if (redoStack.size() == 0) return false;
        
        // if so, close and start redoing
        closeCurrentChangeSetOnly();
        while (true) { 
            // the first set is always the action changeSet
            ChangeSet redoSet = redoStack.removeFirst();
            redoSet.reexecute();
            undoStack.addFirst(redoSet);
            // break if stack is empty the next ActionChangeSet is in view
            if (redoStack.size() == 0 || redoStack.peekFirst() instanceof ActionChangeSet) break;
        }
        startAutoChangeSet(false);
        return true;
    }
    
    /**
     * Update the relevant observers
     */
    private void updateObservers(Set<ChangeSet> changeSets) {
        Set<State> states = Sets.newHashSet();
        for (ChangeSet cs:changeSets) {
            states.addAll(cs.getStates());
        }
        stateManager.updateObservers(states);
    }
    
    /**
     * @return size of ChangeStack
     */
    public int sizeUndoStack() {
        return undoStack.size();
    }
    
}
