package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * A TableMultiCoordinate is built from a list/iterable of items
 */
public class TableMultiCoordinate extends TableCoordinate {

    private final Iterable<Item> items;
    
    protected TableMultiCoordinate(Iterable<Item> items, String id) {
        super(id);
        this.items = items;
    }
    
    public Iterable<Item> getItems() {
        return items;
    }
    
}
