/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/Move.java,v 1.1 2006/07/17 22:00:15 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.action;

/**
 * @author Erik Vos
 */
public abstract class Move {

    public abstract boolean execute ();
    
    public abstract boolean undo();
    
    
}
