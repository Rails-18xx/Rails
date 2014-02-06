package net.sf.rails.game.model;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.Model;

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
