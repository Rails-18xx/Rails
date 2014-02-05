package rails.game;

import rails.game.state.Manager;

/**
 * RailsManager adds Rails specific methods to Manager
 */
public abstract class RailsManager extends Manager implements RailsItem {

    protected RailsManager(RailsItem parent, String id) {
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
