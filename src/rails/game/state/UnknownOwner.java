package rails.game.state;

/**
 * Used to initialize ownable items with an owner
 * PortfolioManager creates a Singleton of this class
 */
public class UnknownOwner extends AbstractItem implements Owner, DelayedItem {

    private UnknownOwner(Item parent, String id) {
        super(parent, id);
    }

    static UnknownOwner create(Item parent, String id) {
        return new UnknownOwner(parent, id);
    }
    
}
