package rails.game.state;

public class AbstractItem implements Item {

    private final String id;
    private final Context root;

    public AbstractItem() {
        id = null;
        root = null;
    }
    
    public AbstractItem(Item owner) {
        this(owner, null);
    }
    
    public AbstractItem(Item owner, String id) {
        if (owner == null) {
            throw new NullPointerException("owner");
        }
        
        this.id = id;

        // pass along the root reference
        this.root = owner.getRoot();
    }
    
    public String getId() {
        return id;
    }

    public Context getRoot() {
        return root;
    }

}
