package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkState;

import java.util.Deque;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ChangeStack {

    private static final Logger log = LoggerFactory.getLogger(ChangeStack.class);

    // static fields
    private final StateManager stateManager;

    private final Deque<ChangeSet> undoStack = Lists.newLinkedList();
    private final Deque<ChangeSet> redoStack = Lists.newLinkedList();

    private ChangeReporter reporter; // assigned once

    // dynamic fields
    private ImmutableList.Builder<Change> changeBuilder;

    private ChangeStack(StateManager stateManager) {
        this.stateManager = stateManager;
        reporter = null;
        changeBuilder = ImmutableList.builder();
    }

    /**
     * Creates a new ChangeStack
     * It is initialized automatically, as there is an open ChangeBuilder
     */
    public static ChangeStack create(StateManager stateManager) {
        ChangeStack changeStack = new ChangeStack(stateManager);
        return changeStack;
    }

    /**
     * Add ChangeReporter
     */
    public void addChangeReporter(ChangeReporter reporter) {
        this.reporter = reporter;
        reporter.init(this);
        log.debug("Added ChangeReporter " + reporter);
    }

    /**
     * @return the previous (closed) changeSet, null if empty
     */
    public ChangeSet getClosedChangeSet() {
        return undoStack.peekLast();
    }

    /**
     * Add change to current changeSet
     */
    void addChange(Change change) {
        log.debug("ChangeSet: Add {}", change);
        changeBuilder.add(change);
        // immediate execution and information of models
        change.execute();
        change.getState().informTriggers(change);
    }

    private boolean checkRequirementsForClose(ChangeAction action) {
        if (changeBuilder.build().isEmpty() || action == null) {
            return false;
        } else {
            return true;
        }
    }

    public void close(ChangeAction action) {
        if (checkRequirementsForClose(action)) {
            // this has to be done before the changeBuilder closes
            int index = undoStack.size() + 1;
            ChangeSet closeSet = new ChangeSet(changeBuilder.build(), action, index);
            log.debug("<<< Closed changeSet {}", closeSet);
            undoStack.addLast(closeSet);
            redoStack.clear();

            if (reporter != null) {
                reporter.updateOnClose();
            }

            // restart builders
            restart();
            // inform direct and indirect observers
            updateObservers(closeSet.getStates());
        }
    }

    private void restart() {
        changeBuilder = ImmutableList.builder();
    }


    public void updateObservers(Set<State> states) {
        // update the observers of states and models
        log.debug("ChangeStack: update Observers");
        stateManager.updateObservers(states);
    }

    // is undo possible (protect first index)
    public boolean isUndoPossible() {
        return (!undoStack.isEmpty() && undoStack.size() != 1);
    }

    public boolean isUndoPossible(ChangeActionOwner owner) {
        return (isUndoPossible() &&
                undoStack.peekLast().getOwner() == owner);
    }

    /**
     * Undo command
     */
    public void undo() {
        checkState(isUndoPossible(), "Undo not possible");
        ChangeSet undoSet = executeUndo();
        restart();
        updateObservers(undoSet.getStates());

        if (reporter != null) {
            reporter.updateAfterUndoRedo();
        }
    }

    /**
     * Example: Undo-Stack has 4 elements (1,2,3,4), size = 4
     * Undo to index 2, requires removing the latest element, such that size = 3
     */

    public void undo(int index) {
        checkState(isUndoPossible() && index < undoStack.size() , "Undo not possible");
        ImmutableSet.Builder<State> states = ImmutableSet.builder();
        while (undoStack.size() > index) {
            states.addAll(executeUndo().getStates());
        }
        restart();
        updateObservers(states.build());
        if (reporter != null) {
            reporter.updateAfterUndoRedo();
        }
    }

    private ChangeSet executeUndo() {
        ChangeSet undoSet = undoStack.pollLast();
        log.debug("UndoSet = " + undoSet);
        undoSet.unexecute();
        redoStack.addFirst(undoSet);

        if (reporter != null) {
            reporter.informOnUndo();
        }

        return undoSet;
    }


    public boolean isRedoPossible() {
        return (!redoStack.isEmpty());
    }

    public boolean isRedoPossible(ChangeActionOwner owner) {
        return (isRedoPossible() &&
                redoStack.peekFirst().getOwner() == owner);
    }

    /**
     * Redo command
     * @throws IllegalStateException if redo stack is empty or there is an open ChangeSet
     */
    public void redo() {
        checkState(isRedoPossible(), "Redo not possible");

        ChangeSet redoSet = executeRedo();
        restart();
        updateObservers(redoSet.getStates());
        if (reporter != null) {
            reporter.updateAfterUndoRedo();
        }
    }

    public void redo(int index) {
        checkState(index > undoStack.size() && index <= undoStack.size() + redoStack.size(),
                "Redo not possible");

        ImmutableSet.Builder<State> states = ImmutableSet.builder();
        while (undoStack.size() < index) {
            states.addAll(executeRedo().getStates());
        }
        restart();
        updateObservers(states.build());
        if (reporter != null) {
            reporter.updateAfterUndoRedo();
        }
    }

    private ChangeSet executeRedo() {
        ChangeSet redoSet = redoStack.pollFirst();
        log.debug("RedoSet = " + redoSet);
        redoSet.reexecute();
        undoStack.addLast(redoSet);

        if (reporter != null) {
            reporter.informOnRedo();
        }

        return redoSet;
    }

    /**
     * @return current index of the ChangeStack (equal to size of undo stack)
     */
    public int getCurrentIndex() {
        return undoStack.size();
    }

    /**
     * @return size of undoStack plus RedoStack
     */
    public int getMaximumIndex() {
        return redoStack.size() + undoStack.size();
    }

}
