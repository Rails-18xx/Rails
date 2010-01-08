/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenI.java,v 1.5 2010/01/08 21:30:46 evos Exp $
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

    public void setHolder(TokenHolder holder);

    public String getUniqueId();

    public TokenHolder getHolder();

    public String getName();

    public boolean equals(TokenI otherToken);

}
