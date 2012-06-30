package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Manager is an abstract implementation of a Context
 */
public abstract class Manager implements Context {
    
    // standard item fields
    private final String id;
    private final Item parent;
    
    protected final Root root;

    // storage of items
    protected final HashMapState<String, Item> items = HashMapState.create(this, "items");

    protected Manager(Item parent, String id) {
        checkNotNull(id, "Id cannot be null");
        this.id = id;

        if (this instanceof Root) {
            this.parent = null;
            this.root = null;
            return;
        }

        // check arguments, parent can only be null for Root
        checkNotNull(parent, "Parent cannot be null");
        this.parent = parent;
        
        // find root and add context there
        root = parent.getContext().getRoot();
        root.addItem(this);
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
        if (parent instanceof Manager) {
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

   public void addItem(Item item) {
        // check if this context is the containing one
        checkArgument(item.getContext() == this, "Context is not the container of the item to add");
        // check if it already exists
        checkArgument(items.containsKey(item.getURI()), "Context already contains item with identical URI");
        
        // all preconditions ok => add item
        items.put(item.getURI(), item);
        
        // add item to root
        root.addItem(item);
    }
   
   public void removeItem(Item item) {
       // check if this context is the containing one
       checkArgument(item.getContext() == this, "Context is not the container of the item to add");
       // check if it is stored
       checkArgument(!items.containsKey(item.getURI()), "Context does not contain item with that URI");
       
       // all preconditions ok => remove item
       items.remove(item.getURI());
       
       // remove item from root
       root.removeItem(item);
   }
    
    public Root getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return parent.toString();
    }
}
