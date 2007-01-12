/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/Move.java,v 1.2 2007/01/12 22:51:29 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import org.apache.log4j.Logger;

/**
 * @author Erik Vos
 */
public abstract class Move {

	protected static Logger log = Logger.getLogger(Move.class.getPackage().getName());

    public abstract boolean execute ();
    
    public abstract boolean undo();
    
    
}
