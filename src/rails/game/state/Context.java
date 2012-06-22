package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contexts allow location of other items
 */
public class Context implements Item {
    // standard item fields
    protected final String id;
    protected final Item parent;
    // context reference to the root
    protected final Root root;

    // storage of items
    protected final HashMapState<String, Item> items = HashMapState.create(this, "items");

    protected Context(Item parent, String id) {
        // check arguments
        checkNotNull(parent, "Parent cannot be null");
        checkNotNull(id, "Id cannot be null");
        checkArgument(id != Root.ID, "Id cannot equal " + Root.ID);
        
        // standard fields
        this.parent = parent;
        this.id = id;
        
        // find root and add context there
        root = parent.getContext().getRoot();
        root.addItemToRoot(this);
    }
    
    public static Context create(Item parent, String id) {
        return new Context(parent, id);
    }
    
    public String getId() {
        return id;
    }

    public Item getParent() {
        return parent;
    }
    
    public Context getContext() {
        return this;
    }
    
    public String getURI() {
        if (parent instanceof Context) {
            return id;
        } else {
            // recursive definition
            return parent.getURI() + Item.SEP + id;
        }
    }

    public String getFullURI() {
        // recursive definition
        return parent.getFullURI() + Item.SEP + id;
    }
    
    // Context methods
    public Item localize(String uri) {
        // either item can be found in the map
        if (items.containsKey(uri)) {
            return items.get(uri);
        } else {
            // otherwise search in root
            return root.localize(uri);
        }
    }

   void addItem(AbstractItem item) {
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
        return parent.toString();
    }
}
