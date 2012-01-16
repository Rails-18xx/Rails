package rails.game.model;

import rails.game.state.GameItem;
import rails.game.state.Item;

/**
 * A simple implementation of an ownable object
 * 
 * @author freystef
 */
public abstract class AbstractOwnable extends GameItem implements Ownable {

    private final OwnerState owner = new OwnerState();

    @Deprecated
    // TODO: Remove that default constructor here
    public AbstractOwnable() {
        super();
    }
    
    public AbstractOwnable(String id){
        super(id);
    }
    
    @Override
    public GameItem init(Item parent){
        super.init(parent);
        owner.init(this);
        return this;
    }
    
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
