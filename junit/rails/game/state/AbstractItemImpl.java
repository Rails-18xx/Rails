package rails.game.state;

// Implementation for Testing only
class AbstractItemImpl extends AbstractItem {
    AbstractItemImpl(Item parent, String id) {
        super(parent, id);
    }
    static AbstractItemImpl create(Item parent, String id) {
        return new AbstractItemImpl(parent, id);
    }
}
