package rails.game.state;

/**
 * Implementation of an Owner used for Testing
 */

class OwnerImpl extends AbstractItem implements Owner {
    
    private OwnerImpl(Item parent, String id) {
        super(parent, id);
    }

    static OwnerImpl create(Item parent, String id) {
        return new OwnerImpl(parent, id);
    }
    
    public String toString() {
        return "Owner(Id=" + getId() + ")";
    }

    
}
