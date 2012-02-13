package rails.game.state;

import static com.google.common.base.Preconditions.*;

/**
 * An AbstractItem is a default implementation of Item
 */
public abstract class AbstractItem implements Item {

    private String id;
    // All nodes reference back to parent and context
    private Item parent;

    private boolean initialized = false;

    // Item interface
    public AbstractItem init(Item parent, String id){
        checkNotNull(parent, "Parent cannot be null");
        checkNotNull(id, "Id cannot be null");
        checkArgument(id != Root.id, "Id cannot equal " + Root.id);

        // defined standard fields
        this.parent = parent;
        this.id = id;

        // add item to context
        parent.getContext().addItem(this);
        
        // init finished
        initialized = true;
        
        return this;
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
        if (parent instanceof Context) {
            return (Context)parent;
        } else {
            // recursive definition
            return parent.getContext();
        }
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
    
    
}
