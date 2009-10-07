/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MoveSet.java,v 1.12 2009/10/07 19:00:38 evos Exp $
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
    private List<Move> reversedMoves = null;
    private boolean undoableByPlayer;
    /** If TRUE, undoing this move will also undo the previous one. */
    private boolean linkedToPrevious = false;

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

        // Create a reversed move list, if not yet done
    	if (reversedMoves == null) {
    		reversedMoves = new ArrayList<Move>(moves);
    		Collections.reverse(reversedMoves);
    	}
        for (Move move : reversedMoves) {
            move.undo();
            log.debug("Undone: " + move);
        }
    }

    protected boolean isEmpty() {
        return moves.isEmpty();
    }

    public void setLinkedToPrevious() {
        linkedToPrevious = true;
    }

    protected boolean isLinkedToPrevious() {
    	return linkedToPrevious;
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
