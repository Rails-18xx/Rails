package rails.game;

import static com.google.common.base.Preconditions.checkArgument;
import rails.game.state.AbstractItem;
import rails.game.state.Item;

/**
 * RailsItem adds Rails specific methods to AbstractItem
 */
public abstract class RailsItem extends AbstractItem {

    /**
     * RailsItem parent has to have a RailsRoot as Root and RailsManager as Context 
     */
    protected RailsItem(Item parent, String id) {
        super(parent, id);
        checkArgument(parent.getRoot() instanceof RailsRoot, "Root has to be Class RailsRoot");
        checkArgument(parent.getContext() instanceof RailsManager, "Context has to extend of Class RailsManager");
    }
    
    /**
     * RailsItem parent can be a RailsItem
     */
    protected RailsItem(RailsItem parent, String id) {
        super(parent, id);
    }

    /**
     * RailsItem parent can be a RailsManager
     */
    protected RailsItem(RailsManager parent, String id) {
        super(parent, id);
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
