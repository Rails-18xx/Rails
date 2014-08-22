package net.sf.rails.game.state;

import com.google.common.base.Preconditions;

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

    public void moveTo(Owner newOwner) {
        Preconditions.checkArgument(newOwner != owner.value(), 
                "New Owner identical to the existing owner" + newOwner);
        

        // check newPortfolio
        Portfolio<T> newPortfolio = pm.getPortfolio(type, newOwner);
        Preconditions.checkArgument(newPortfolio != null, "No Portfolio available for owner " + newOwner);
        
        // create change for new portfolio
        newPortfolio.include(type.cast(this));
        
        //  remove from old portfolio
        if (owner.value() != pm.getUnkownOwner()) {
            Portfolio<T> oldPortfolio = pm.getPortfolio(type, owner.value());
            oldPortfolio.exclude(type.cast(this));
        }

        // and change the owner
        owner.set(newOwner);
    }
    
    public Owner getOwner() {
        return owner.value();
    }
    
    public void triggeredOnOwnerChange(Triggerable t) {
        owner.addTrigger(t);
    }

    public int compareTo(Ownable other) {
        return this.getId().compareTo(other.getId());
    }

}
