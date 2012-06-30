package rails.game.state;

import static com.google.common.base.Preconditions.*;

/**
 * An AbstractItem is a default implementation of Item
 */
public abstract class AbstractItem implements Item {

    private final String id;
    private final Item parent;

    protected AbstractItem(Item parent, String id){
        checkNotNull(parent, "Parent cannot be null");
        checkNotNull(id, "Id cannot be null");
        checkArgument(id != Root.ID, "Id cannot equal " + Root.ID);

        // defined standard fields
        this.parent = parent;
        this.id = id;

        // add item to context
        parent.getContext().addItem(this);
    }
    
    public String getId() {
        return id;
    }

    public Item getParent() {
        return parent;
    }

    public Context getContext() {
        if (parent instanceof Manager) {
            return (Manager)parent;
        } else {
            // recursive definition
            return parent.getContext();
        }
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
    
}
