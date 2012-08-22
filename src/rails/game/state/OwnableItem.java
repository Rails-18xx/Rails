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

    public boolean moveTo(Owner newOwner) {
        if (newOwner == owner.value()) return false;
        
        // move from old to new portfolio
        Portfolio<T> oldPortfolio = pm.getPortfolio(type, owner.value());
        Portfolio<T> newPortfolio = pm.getPortfolio(type, newOwner);
        // check newPortfolio
        checkNotNull(newPortfolio, "No Portfolio available for owner " + newOwner);
        new PortfolioChange<T>(newPortfolio, oldPortfolio, type.cast(this));
        // and change the owner
        owner.set(newOwner);
        return true;
    }
    
    public Owner getOwner() {
        return owner.value();
    }

    public int compareTo(Ownable other) {
        return this.getId().compareTo(other.getId());
    }

}
