package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Manager is a baseline implementation of a Context
 */
public abstract class Manager extends Context {

    // item fields
    private final String id;
    private final Item parent;
    // context fields
    private final Root root;
    private final String fullURI;

    protected Manager(Item parent, String id) {
        checkNotNull(id, "Id cannot be null");
        this.id = id;

        // check arguments, parent can only be null for Root
        checkNotNull(parent, "Parent cannot be null");
        this.parent = parent;

        // URI defined recursively
        fullURI =  parent.getFullURI() + Item.SEP + id;

        // find root and add context there
        root = parent.getContext().getRoot();
        // add to root
        root.addItem(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Item getParent() {
        return parent;
    }

    @Override
    public Context getContext() {
        if (parent instanceof Context) {
            return (Context)parent;
        } else {
            // recursive definition
            return parent.getContext();
        }
    }

    @Override
    public String getURI() {
        if (parent instanceof Context) {
            return id;
        } else {
            // recursive definition
            return parent.getURI() + Item.SEP + id;
        }
    }

    @Override
    public String getFullURI() {
        return fullURI;
    }

    @Override
    public String toText() {
        return id;
    }

    // Context methods
    @Override
    public Item locate(String uri) {
        // first try as fullURI
        Item item = root.locateFullURI(uri);
        if (item != null) return item;
        // otherwise as local
        return root.locateFullURI(fullURI + Item.SEP + uri);
    }


   @Override
   void addItem(Item item) {
        // check if this context is the containing one
        checkArgument(item.getContext() == this, "Context is not the container of the item to add");

        // add item to root
        root.addItem(item);
   }

   @Override
   void removeItem(Item item) {
       // check if this context is the containing one
       checkArgument(item.getContext() == this, "Context is not the container of the item to add");

       // remove item from root
       root.removeItem(item);
   }

    @Override
    public Root getRoot() {
        return root;
    }
}
