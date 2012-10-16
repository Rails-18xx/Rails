package rails.game.model;

import rails.game.RailsItem;
import rails.game.RailsRoot;
import rails.game.state.Model;

/**
 * RailsModel adds Rails specific methods to Model
 */
public class RailsModel extends Model implements RailsItem {

    protected RailsModel(RailsItem parent, String id) {
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
