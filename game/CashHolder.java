/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CashHolder.java,v 1.4 2006/02/04 01:12:51 wakko666 Exp $
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
	public abstract boolean addCash(int amount);
	
	/** Get the cash owner's name (needed for logging) */
	public abstract String getName();
}