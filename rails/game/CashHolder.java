package rails.game;

import rails.game.model.ModelObject;

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