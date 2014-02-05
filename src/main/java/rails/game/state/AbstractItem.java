package rails.game.state;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Objects;

/**
 * An AbstractItem is a default implementation of Item
 */
public abstract class AbstractItem implements Item {

    private final String id;
    private final Item parent;
    private final Context context;

    protected AbstractItem(Item parent, String id){
        checkNotNull(parent, "Parent cannot be null");
        checkArgument(id != Root.ID, "Id cannot equal " + Root.ID);

        // defined standard fields
        this.parent = parent;
        this.id = id;
        
        if (parent instanceof Context) {
            context = (Context)parent;
        } else { 
            // recursive definition
            context = parent.getContext();
        }

        // add item to context
        context.addItem(this);
    }
    
    public String getId() {
        return id;
    }

    public Item getParent() {
        return parent;
    }

    public Context getContext() {
        return context;
    }
    
    public Root getRoot() {
        // forward it to the context
        return context.getRoot();
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

    public String toText() {
        return id;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("URI", getFullURI()).toString();
    }
    
}
