/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenI.java,v 1.1 2007/01/23 21:50:41 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

/**
 * @author Erik Vos
 */
public interface TokenI {

    public void setHolder (TokenHolderI holder);
    
    public TokenHolderI getHolder ();
    
    public String getName();
}
