/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CashHolder.java,v 1.1 2005/04/16 22:51:22 evos Exp $
 * 
 * Created on 10-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

/**
 * @author Erik
 */
public interface CashHolder {

	/**
	 * Returns the amount of cash.
	 * @return current amount. 
	 */
	public abstract int getCash();
	/**
	 * Add (or subtract) cash.
	 */
	public abstract void addCash(int amount);
	
	/** Get the cash owner's name (needed for logging) */
	public abstract String getName();
}