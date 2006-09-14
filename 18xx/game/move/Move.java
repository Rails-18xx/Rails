/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/Move.java,v 1.1 2006/09/14 19:33:31 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

/**
 * @author Erik Vos
 */
public abstract class Move {

    public abstract boolean execute ();
    
    public abstract boolean undo();
    
    
}
