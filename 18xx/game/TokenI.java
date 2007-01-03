/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TokenI.java,v 1.1 2007/01/03 22:34:16 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public interface TokenI {

    public void setHolder (TokenHolderI holder);
    
    public TokenHolderI getHolder ();
    
    public String getName();
}
