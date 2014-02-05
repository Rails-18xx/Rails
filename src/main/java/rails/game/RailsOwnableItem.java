package rails.game;

import rails.game.model.PortfolioModel;
import rails.game.state.Ownable;
import rails.game.state.OwnableItem;

/**
 * RailsOwnableItem is the rails specific version of RailsOwnableItem
 */

public class RailsOwnableItem<T extends Ownable> extends OwnableItem<T> implements RailsItem {

    protected RailsOwnableItem(RailsItem parent, String id, Class<T> type) {
        super(parent, id, type);
    }

    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }

    /**
     * Moves the item to the owner of the portfolioModel
     * @param model the model of the new owner 
     */
    public void moveTo(PortfolioModel model) {
        moveTo(model.getParent());
    }
}
