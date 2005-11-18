package game;

import java.util.*;

/**
 * Interface for implementing a TokenHolder
 * 
 * A TokenHolder is any object that can have a token played upon it.
 * 
 * @author Brett
 *
 */
public interface TokenHolderI
{
	/**
	 * Add a token to our pile.
	 * @param company
	 */
	public void addToken(CompanyI company);
	
	/**
	 * Remove a token from the pile.
	 * 
	 * @param company
	 *            The company object to remove.
	 * @return False if the token was not found.
	 */
	public boolean removeToken(CompanyI company);
	
	/**
	 * @return ArrayList of all tokens we have.
	 */
	public List getTokens();
	
	/**
	 * Do we have any tokens?
	 * @return Boolean 
	 */
	public boolean hasTokens();
}
