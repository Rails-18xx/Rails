package rails.game;

import rails.game.state.Owner;

/**
 * RailsOwner is the Rails specific version of Owner
 */
public interface RailsOwner extends Owner, RailsItem {
 
    // Remark: Both methods have to be redefined here to avoid ambiguity (due to extending from Item => Owner and RailsItem)
    public RailsItem getParent();
    public RailsRoot getRoot();
}
