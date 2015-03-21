package net.sf.rails.ui.swing.core;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * The Root UI is the top element of the UI hierarchy
 */
public class RootUI extends ItemUI {

    public final static String ID = ""; 

    private final static String TEXT_ID = "rootUI";
    
    private static final String PRECON_ITEM_UI_CONTAINED = "ItemUI %s already contained in UI hierarchy";

    private final Map<String, ItemUI> items = Maps.newHashMap();
    
    private RootUI() {
        super(null, ID);
        items.put(ID, this);
    }
    
    protected void addItem(ItemUI item) {
        Preconditions.checkArgument(!items.containsKey(item.getURI()), PRECON_ITEM_UI_CONTAINED, item.getURI());
        items.put(item.getURI(), item);
    }
    
    // ItemUI methods
    
    /**
     * @throws UnsupportedOperationsException
     * Not supported for RootUI
     */
    @Override
    public ItemUI getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return ID;
    }
    
    @Override
    public RootUI getRoot() {
        return this;
    }
    
    @Override
    public String getURI() {
        return ID;
    }

    @Override
    public String toString() {
        return TEXT_ID;
    }
    
}
