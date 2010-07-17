/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TokenHolder.java,v 1.1 2010/01/08 21:30:46 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.move.MoveableHolder;

/**
 * Interface for implementing a TokenHolder
 *
 * A TokenHolder is any object that can have a token played upon it.
 */
public interface TokenHolder extends MoveableHolder {

    /*
     * Add a token. Subclasses may override this method to implement side
     * effects.
     *
     * @param token The token object to add.
     * @return True if successful.
     */
    public boolean addToken(TokenI token, int position);

    /**
     * Remove a token. Subclasses may override this method to implement side
     * effects.
     *
     * @param token The token object to remove.
     * @return True if successful.
     */
    public boolean removeToken(TokenI token);

    /**
     * @return ArrayList of all tokens we have.
     */
    public List<TokenI> getTokens();

    /**
     * Do we have any tokens?
     *
     * @return Boolean
     */
    public boolean hasTokens();

    /**
     * Each station must have a name, which includes the tile Id (if on a tile)
     * or the hex name (if on a MapHex).
     *
     * @return
     */
    public String getName();
}
