package rails.game.state;

/** 
 * AbstractItem is the default implementation of an Item
 * @author freystef
 */

public class AbstractItem implements Item {

    private final String id;
    
    // parent is set once at time of initialization
    private Item parent = null;
    
    @Deprecated
    // TODO: Remove that default constructor here
    public AbstractItem() {
        this.id = null;
    }
    
    /**
     * Creates an AbstractItem
     * @param id identifier for the item (cannot be null)
     */
    public AbstractItem(String id){
        if (id == null) {
            throw new IllegalArgumentException("Missing id for an item in hierarchy");
        }
        this.id = id;
    }

    public void init(Item parent){
        if (this.parent != null) {
            throw new IllegalStateException("Item already intialized");
        }
        this.parent = parent;
    }
    
    public String getId() {
        return id;
    }
    
    public Item getParent() {
        if (parent == null) {
            throw new IllegalStateException("Item not yet intialized");
        }
        return parent;
    }

    public Context getContext() {
        if (parent == null) {
            throw new IllegalStateException("Item not yet intialized");
        }
        return parent.getContext();
    }
    
    public String getURI() {
        if (parent != null && parent.getURI() != null ) {
            if (parent instanceof Context) {
                return id;
            } else {
                return parent.getURI() + Context.SEP + id;
            }
        } else {
            return id;
        }
    }
}
