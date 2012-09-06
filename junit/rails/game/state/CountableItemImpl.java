package rails.game.state;

class CountableItemImpl extends CountableItem<Countable> {
    
    protected CountableItemImpl(Item parent, String id) {
        super(parent, id, Countable.class);
    }

    static CountableItemImpl create(Item parent, String id) {
        return new CountableItemImpl(parent, id);
    }
    
}
