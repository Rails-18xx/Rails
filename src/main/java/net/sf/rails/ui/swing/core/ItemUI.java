package net.sf.rails.ui.swing.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Defines a general UI item part of the UI hierarchy
 */
public abstract class ItemUI {

    public static final char SEP = '%';

    private final String id;
    private final ItemUI parent;
    private final RootUI root;
    
    protected ItemUI(ItemUI parent, String id){
        if (this  instanceof RootUI) {
            // only used for RootUI
            this.root = (RootUI)this;
        } else {
            // all others, check preconditions
            Preconditions.checkNotNull(parent, "parent cannot be null");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id cannot be null or empty");
            this.root = parent.getRoot();
        }
        this.id = id;
        this.parent = parent;
        root.addItem(this);
   }


    public ItemUI getParent() {
        return parent;
    }
    
    public RootUI getRoot() {
        return root;
    }

    public String getId() {
        return id;
    }
    
    public String getURI() {
        return parent.getURI() + SEP + id;
    }
    
    @Override
    public String toString() {
        return getURI();
    }
    
}
