package rails.game;

import rails.game.state.Item;

/**
 * Adapts Item to the Rails environment
 */
public interface RailsItem extends Item {

    public RailsItem getParent();
    
    public RailsRoot getRoot();
    
}
