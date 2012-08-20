package rails.game;

import static com.google.common.base.Preconditions.checkArgument;
import rails.game.state.Item;
import rails.game.state.Manager;

/**
 * RailsItem adds Rails specific methods to AbstractItem
 */
public abstract class RailsManager extends Manager {

    protected RailsManager(Item parent, String id) {
        super(parent, id);
        checkArgument(parent.getRoot() instanceof RailsRoot, "Root has to be of Class RailsRoot");
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }
    
    @Override
    public RailsManager getContext() {
        return (RailsManager)super.getContext();
    }
}
