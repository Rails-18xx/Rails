package rails.game.state;

public abstract class OwnableItem<T extends Ownable> extends AbstractItem implements Ownable {
    
    private Class<T> type;
    
    protected OwnableItem(Item parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
    }

    // This unchecked warning is required as the type parameter above is 
    // used to define the type of portfolios the items are stored
    @SuppressWarnings("unchecked")
    public void moveTo (Owner newOwner) {
        getRoot().getStateManager().getPortfolioManager().moveItem(type, (T) this, newOwner);
    }
}
