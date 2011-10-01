package rails.game.model;

import rails.game.state.AbstractItem;

/**
 * A simple implementation of an ownable object
 * 
 * @author freystef
 */
public abstract class AbstractOwnable extends AbstractItem implements Ownable {

    private final OwnerState owner = new OwnerState(this);
    
    // Ownable Interface methods
    public final void moveTo(Owner newOwner) {
        Owners.move(this, newOwner);
    }
    
    public final void setOwner(Owner newOwner) {
        owner.set(newOwner);
    }
    
    public final Owner getOwner() {
        return owner.get();
    }
    
}
