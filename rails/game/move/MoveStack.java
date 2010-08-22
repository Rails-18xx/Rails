/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MoveStack.java,v 1.4 2010/01/31 22:22:30 macfreek Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.ReportBuffer;
import rails.util.LocalText;

/**
 * This class represent one game's complete "move stack", which is a list
 * of MoveSets. Each MoveSet contains the (low-level) changes caused by
 * one particular player action.
 * <p>The only purpose of the move stack is to enable Undo and Redo.
 * @author Erik Vos
 */
public class MoveStack {

    private MoveSet currentMoveSet = null;
    private List<MoveSet> moveStack = new ArrayList<MoveSet>();
    private int lastIndex = -1;
    private boolean enabled = false;

    protected static Logger log =
            Logger.getLogger(MoveStack.class.getPackage().getName());

    public MoveStack () {
    }

    /**
     * Start making moves undoable. Will be called once, after all
     * initialisations are complete.
     */
    public void enable() {
        enabled = true;
    }

    public MoveSet start(boolean undoableByPlayer) {
        log.debug(">>> Start MoveSet(index=" + (lastIndex + 1) + ")");
        if (currentMoveSet == null) {
            currentMoveSet = new MoveSet(undoableByPlayer);
            while (lastIndex < moveStack.size() - 1) {
                moveStack.remove(moveStack.size() - 1);
            }
            ReportBuffer.createNewReportItem(getCurrentIndex());
            return currentMoveSet;
        } else {
            log.warn("MoveSet is already open");
            return currentMoveSet;
        }
    }

    public boolean finish() {
        log.debug("<<< Finish MoveSet (index=" + (lastIndex + 1) + ")");
        if (currentMoveSet == null) {
            log.warn("No action open for finish");
            return false;
        } else if (currentMoveSet.isEmpty()) {
            log.warn("Action to finish is empty and will be discarded");
            currentMoveSet = null;
            return true;
        } else {
            moveStack.add(currentMoveSet);
            lastIndex++;
            currentMoveSet = null;
            return true;
        }
    }

    public boolean cancel() {
        if (currentMoveSet != null) {
            currentMoveSet.unexecute();
            currentMoveSet = null;
            return true;
        } else {
            log.warn("No action open for cancel");
            return false;
        }
    }

    public boolean addMove (Move move) {

        if (!enabled) return true;

        if (currentMoveSet != null) {
            currentMoveSet.addMove(move);
            return true;
        } else {
            log.warn("No MoveSet open for " + move);
            return false;
        }
    }

    public void linkToPreviousMoveSet() {
        if (currentMoveSet != null) {
            currentMoveSet.linkToPreviousMoveSet();
            log.debug("Moveset linked to previous one");
        } else {
            log.warn("No MoveSet open");
        }
    }

    public boolean undoMoveSet (boolean forced) {
        if (lastIndex >= 0 && lastIndex < moveStack.size()
                && (forced || moveStack.get(lastIndex).isUndoableByPlayer())
                && currentMoveSet == null) {
            MoveSet undoAction;
            do {
                ReportBuffer.add(LocalText.getText("UNDO"));
                log.debug ("MoveStack undo index is "+lastIndex);
                undoAction = moveStack.get(lastIndex--);
                undoAction.unexecute();
            } while (undoAction.isLinkedToPreviousMove());
            
            return true;
        } else {
            log.error("Invalid undo: index=" + lastIndex + " size="
                      + moveStack.size() + " currentAction=" + currentMoveSet
                      + " forced=" + forced + " byPlayer="
                      + isUndoableByPlayer());
            return false;
        }
    }

    public boolean redoMoveSet () {
        if (currentMoveSet == null && lastIndex < moveStack.size() - 1) {
            MoveSet redoAction;
            do {
                redoAction = moveStack.get(++lastIndex);
                ReportBuffer.add(LocalText.getText("REDO"));
                log.debug ("MoveStack redo index is "+lastIndex);
                redoAction.reexecute();
                if (lastIndex == moveStack.size() - 1) break;
            } while (moveStack.get(lastIndex + 1).isLinkedToPreviousMove());
            return true;
        } else {
            log.error("Invalid redo: index=" + lastIndex + " size="
                      + moveStack.size());
            return false;
        }
    }

    public boolean isUndoableByPlayer() {
        return lastIndex >= 0 && moveStack.get(lastIndex).isUndoableByPlayer();
    }

    public boolean isRedoable() {
        return lastIndex < moveStack.size() - 1;
    }

    public boolean isUndoableByManager() {
        return lastIndex >= 0;
    }

    public boolean isOpen() {
        return currentMoveSet != null;
    }
    
    public int getIndex() {
        return lastIndex + 1;
    }
    
    /** 
     * the current index is the one of either the open moveset or
     * if none is open of the latest added
     */
    public int getCurrentIndex() {
        if (isOpen()) {
            return lastIndex + 1;
        } else {
            return lastIndex;
        }
    }
    
    public int size() {
        return moveStack.size();
    }
    
    /**
     * undo/redo to a given moveStack index
     */
    public boolean gotoIndex(int index) {
        if (getIndex() == index) return true;
        else if (getIndex() < index) {
            while (getIndex() < index) {
                if (!redoMoveSet()) return false;
            }
        } else {
            while (getIndex() > index) {
                if (!undoMoveSet(true)) return false;
            }
        };
        return true;
    }
}
