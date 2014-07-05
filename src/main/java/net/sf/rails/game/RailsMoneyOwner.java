package net.sf.rails.game;

import net.sf.rails.game.state.MoneyOwner;

/**
 * RailsMoneyOwner is the Rails specific version of MoneyOwner
 */
public interface RailsMoneyOwner extends MoneyOwner, RailsItem {
 
    // Remark: Both methods have to be redefined here to avoid ambiguity (due to extending from Item => MoneyOwner and RailsItem)
    public RailsItem getParent();
    public RailsRoot getRoot();
}
