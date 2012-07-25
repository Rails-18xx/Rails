package rails.game.state;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OwnableItem<T extends Ownable> extends AbstractItem implements Ownable {
    
    private final Class<T> type;
    private final PortfolioManager pm;
    private final GenericState<Owner> owner = GenericState.create(this, "owner");
    
    /**
     * Initializes OwnableItem
     * @param parent parent is usually a factory that creates the OwnableItem(s)  
     * @param id identifier of the item
     * @param type indicates the class used for the portfolios to store this type of OwnableItems
     */
    protected OwnableItem(Item parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.pm = getRoot().getStateManager().getPortfolioManager();
        this.owner.set(pm.getUnkownOwner());
    }

    /**
     * Moves the ownable (item) to the new owner  
     * @param newOwner the new Owner of the Item
     * @throws NullPointerException if the new owner has no portfolio which accepts the item
     */
    public void moveTo(Owner newOwner) {
        // move from old to new portfolio
        Portfolio<T> oldPortfolio = pm.getPortfolio(type, owner.value());
        Portfolio<T> newPortfolio = pm.getPortfolio(type, newOwner);
        // check newPortfolio
        checkNotNull(newPortfolio, "No Portfolio available for owner " + newOwner);
        new PortfolioChange<T>(newPortfolio, oldPortfolio, type.cast(this));
        // and change the owner
        owner.set(newOwner);
    }
    
    public Owner getOwner() {
        return owner.value();
    }
}
