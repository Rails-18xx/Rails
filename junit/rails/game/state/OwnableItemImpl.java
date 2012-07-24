package rails.game.state;

/**
 * Test implementation of OwnableItem
 */
class OwnableItemImpl extends OwnableItem<OwnableItemImpl> {

    private OwnableItemImpl(Item parent, String id) {
        super(parent, id, OwnableItemImpl.class);
    }

    static OwnableItemImpl create(Item parent, String id) {
        return new OwnableItemImpl(parent, id);
    }
    
    public String toString() {
        return "OwnableItem(Id=" + getId() + ")";
    }
    
}
