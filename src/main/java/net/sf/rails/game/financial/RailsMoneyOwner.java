package net.sf.rails.game.financial;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.MoneyOwner;

/**
 * RailsMoneyOwner is the Rails specific version of MoneyOwner
 */
public interface RailsMoneyOwner extends MoneyOwner, RailsItem {
 
    // Remark: Both methods have to be redefined here to avoid ambiguity (due to extending from Item => MoneyOwner and RailsItem)
    public RailsItem getParent();
    public RailsRoot getRoot();
}
