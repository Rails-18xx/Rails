/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/CashHolder.java,v 1.4 2008/06/04 19:00:30 evos Exp $
 */
package rails.game;

import rails.game.model.ModelObject;

public interface CashHolder {

    /**
     * Returns the amount of cash.
     * 
     * @return current amount.
     */
    public abstract int getCash();

    public ModelObject getCashModel();

    /**
     * Add (or subtract) cash.
     */
    public abstract void addCash(int amount);

    /** Get the cash owner's name (needed for logging) */
    public abstract String getName();
}