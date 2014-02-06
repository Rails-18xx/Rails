package net.sf.rails.game;

import net.sf.rails.game.state.AbstractItem;

/**
 * RailsAbstractItem adds Rails specific methods to AbstractItem
 */
public abstract class RailsAbstractItem extends AbstractItem implements RailsItem {

    protected RailsAbstractItem(RailsItem parent, String id) {
        super(parent, id);
    }
    
    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }
    
}
