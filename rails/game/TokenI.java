/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenI.java,v 1.5 2010/01/08 21:30:46 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import rails.game.state.Moveable;
import rails.game.state.Holder;

/**
 * @author Erik Vos
 */
public interface TokenI extends Moveable {

    public void setHolder(TokenHolder holder);

    public String getUniqueId();

    public Holder getHolder();

    public String getId();

    public boolean equals(TokenI otherToken);

}
