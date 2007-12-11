/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenI.java,v 1.2 2007/12/11 20:58:33 evos Exp $
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
    
    public String getUniqueId ();

    public TokenHolderI getHolder ();
    
    public String getName();
}
