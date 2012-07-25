package rails.game.state;

/**
 * Test implementation of OwnableItem
 */
class OwnableItemImpl extends OwnableItem<Ownable> {

    private OwnableItemImpl(Item parent, String id) {
        super(parent, id, Ownable.class);
    }

    static OwnableItemImpl create(Item parent, String id) {
        return new OwnableItemImpl(parent, id);
    }
    
}
