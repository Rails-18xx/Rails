package rails.game.state;

// Implementation of a typed OwnableItem
class TypeOwnableItemImpl extends OwnableItem<TypeOwnableItemImpl> implements Typable<String> {
    private final String type;

    private TypeOwnableItemImpl(Item parent, String id, String type) {
        super(parent, id, TypeOwnableItemImpl.class);
        this.type = type;
    }

    static TypeOwnableItemImpl create(Item parent, String id, String type) {
        return new TypeOwnableItemImpl(parent, id, type);
    }
   
    public String getType() {
        return type;
    }
}
