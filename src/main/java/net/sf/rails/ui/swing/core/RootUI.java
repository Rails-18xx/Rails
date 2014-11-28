package net.sf.rails.ui.swing.core;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * The Root UI is the top element of the UI hierarchy
 */
public class RootUI extends ItemUI {

    public final static String ID = ""; 
    
    private final static String TEXT_ID = "rootUI";

    private final Map<String, ItemUI> items = Maps.newHashMap();
    
    private RootUI() {
        super();
        items.put(ID, this);
    }
    
    protected void addItem(ItemUI item) {
        items.put(item.getId(), item);
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
