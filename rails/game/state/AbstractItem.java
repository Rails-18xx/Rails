package rails.game.state;

public class AbstractItem implements Item {

    private final String id;
    private final Context root;

    public AbstractItem() {
        id = null;
        root = null;
    }
    
    public AbstractItem(Item parent) {
        this(parent, null);
    }
    
    public AbstractItem(Item parent, String id) {
        if (parent == null) {
            throw new NullPointerException("owner");
        }
        
        this.id = id;

        // pass along the root reference
        this.root = parent.getRoot();
    }
    
    public String getId() {
        return id;
    }

    public Context getRoot() {
        return root;
    }

}
