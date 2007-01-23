package rails.game;

import java.util.*;

/**
 * Interface for implementing a TokenHolder
 * 
 * A TokenHolder is any object that can have a token played upon it.
 */
public interface TokenHolderI
{

	/**
	 * Add a token to our pile.
	 * 
	 * @param company
	 * @deprecated
	 */
	//public boolean addToken(TokenHolderI company);

	/**
	 * Remove a token from the pile.
	 * 
	 * @param company
	 *            The company object to remove.
	 * @return False if the token was not found.
	 * @deprecated
	 */
	//public boolean removeToken(TokenHolderI company);

	/** Add a token. 
	 * Subclasses may override this method to implement side effects.
	 * @param token The token object to add.
	 * @return True if successful.
	 */
	public boolean addToken (TokenI token);
	
	/** Remove a token.
	 * Subclasses may override this method to implement side effects.
	 * @param token The token object to remove.
	 * @return True if successful.
	 */
	public boolean removeToken (TokenI token);

	/**
	 * @return ArrayList of all tokens we have.
	 */
	public List getTokens();

	/**
	 * Do we have any tokens?
	 * 
	 * @return Boolean
	 */
	public boolean hasTokens();
	
	/**
	 * Each station must have a name, which includes the tile Id (if on a tile)
	 * or the hex name (if on a MapHex).
	 * @return
	 */
	public String getName();
}
