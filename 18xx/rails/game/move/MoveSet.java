/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MoveSet.java,v 1.14 2010/01/31 22:22:30 macfreek Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.*;

import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.GameManagerI;

/**
 * @author Erik Vos
 */
public class MoveSet {

    private List<Move> moves = new ArrayList<Move>();
    private boolean undoableByPlayer;
    /** If TRUE, undoing this move will also undo the previous one. */
    private boolean linkedToPreviousMoveSet = false;

    protected static Logger log =
            Logger.getLogger(MoveSet.class.getPackage().getName());

    protected MoveSet(boolean undoableByPlayer) {
        this.undoableByPlayer = undoableByPlayer;
    }

    protected void addMove (Move move) {
        moves.add(move);
        log.debug("Done: " + move);
    }

    protected void reexecute() {

        for (Move move : moves) {
            move.execute();
            log.debug("Redone: " + move);
        }
    }

    protected void unexecute() {

        // Create a reversed move list
        List<Move> reversedMoves = new ArrayList<Move>(moves);
        Collections.reverse(reversedMoves);
        for (Move move : reversedMoves) {
            move.undo();
            log.debug("Undone: " + move);
        }
    }

    protected boolean isEmpty() {
        return moves.isEmpty();
    }

    public void linkToPreviousMoveSet() {
        linkedToPreviousMoveSet = true;
    }

    protected boolean isLinkedToPreviousMove() {
        return linkedToPreviousMoveSet;
    }

    protected boolean isUndoableByPlayer () {
        return undoableByPlayer;
    }

    /* Static methods to enable access from anywhere */

    protected static void add (Move move) {
        move.execute();

        MoveStack moveStack = getMoveStack();
        if (moveStack != null) moveStack.addMove(move);
    }

    private static MoveStack getMoveStack () {
        GameManagerI gameManager = GameManager.getInstance();
        if (gameManager != null) {
            return gameManager.getMoveStack();
        } else {
            // No GM during game setup; no problem, as MoveSets are not yet enabled then.
            return null;
        }
    }

}
