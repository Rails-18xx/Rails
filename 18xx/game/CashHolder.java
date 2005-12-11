/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CashHolder.java,v 1.3 2005/12/11 00:03:36 evos Exp $
 * 
 * Created on 10-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

import game.model.ModelObject;

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
	 * Returns the amount of cash, formatted
	 * @return
	 */
	public abstract String getFormattedCash();

	public ModelObject getCashModel ();

	/**
	 * Add (or subtract) cash.
	 */
	public abstract void addCash(int amount);
	
	/** Get the cash owner's name (needed for logging) */
	public abstract String getName();
}