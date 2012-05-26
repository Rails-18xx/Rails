package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Contexts allow location of other items
 */
public class Context implements Item {

    // standard item fields
    private String id;
    private Item parent;
    // reference to the root
    private Root root;

    // storage of items
    private HashMapState<String, Item> items;

    private boolean initialized = false;

    protected Context() {}
    
    /**
     * Creates a default context
     */
    public static Context create() {
        return new Context();
    }
    
    // Item interface
    public void init(Item parent, String id) {
        checkNotNull(parent, "Parent cannot be null");
        checkNotNull(id, "Id cannot be null");
        checkArgument(id != Root.id, "Id cannot equal " + Root.id);
        
        // standard fields
        this.parent = parent;
        this.id = id;

        // add context to root
        root.addItemToRoot(this);
        
        // create item store
        items = HashMapState.create();
        
        // init finished
        initialized = true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
   public String getId() {
        checkState(initialized, "Item not yet initialized");
        return id;
    }

    public Item getParent() {
        checkState(initialized, "Item not yet initialized");
        return parent;
    }
    
    public Context getContext() {
        checkState(initialized, "Item not yet initialized");
        return this;
    }
    
    public String getURI() {
        checkState(initialized, "Item not yet initialized");
        if (parent instanceof Context) {
            return id;
        } else {
            // recursive definition
            return parent.getURI() + Item.SEP + id;
        }
    }

    public String getFullURI() {
        checkState(initialized, "Item not yet initialized");
        // recursive definition
        return parent.getFullURI() + Item.SEP + id;
    }
    
    // Context methods
    public Item localize(String uri) {
        checkState(initialized, "Context not yet initialized");
        // either item can be found in the map
        if (items.containsKey(uri)) {
            return items.get(uri);
        } else {
            // otherwise search in root
            return root.localize(uri);
        }
    }

   void addItem(AbstractItem item) {
        checkState(initialized, "Context not yet initialized");
        // check if this context is the containing one
        checkArgument(item.getContext() == this, "Context is not the container of the item to add");
        // check if it already exists
        checkArgument(items.containsKey(item.getURI()), "Context already contains item with identical URI");
        
        // all preconditions ok => add item
        items.put(item.getURI(), item);
        
        // add item to root
        root.addItemToRoot(item);
    }
   
    
    public Root getRoot() {
        return root;
    }
    

    @Override
    public String toString() {
        if (initialized) {
            return id;
        } else {
            return parent.toString();
        }
    }

    
}
