/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenI.java,v 1.3 2007/12/21 21:18:12 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import rails.game.move.Moveable;

/**
 * @author Erik Vos
 */
public interface TokenI extends Moveable {

    public void setHolder (TokenHolderI holder);
    
    public String getUniqueId ();

    public TokenHolderI getHolder ();
    
    public String getName();
    
    public boolean equals (TokenI otherToken);

}
