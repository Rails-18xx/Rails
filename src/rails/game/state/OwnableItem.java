package rails.game.state;

public abstract class OwnableItem<T extends Ownable> extends AbstractItem implements Ownable {
    
    private Class<T> type;
    private Owner owner;
    
    /**
     * TODO: Currently all OwnableItem have Owner set to null at init
     * Should be changed that all ownable Items have an owner at the time of creation (a kind of NULL-Owner)
     */
    protected OwnableItem(Item parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.owner = null;
    }

    // This unchecked warning is required as the type parameter above is 
    // used to define the type of portfolios the items are stored
    @SuppressWarnings("unchecked")
    public void moveTo(Owner newOwner) {
        getRoot().getStateManager().getPortfolioManager().moveItem(type, (T) this, newOwner);
        owner = newOwner;
    }
    
    public Owner getOwner() {
        return owner;
    }
}
