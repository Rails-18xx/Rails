package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * A GridMultiCoordinate is built from a list/iterable of items
 */
public class GridMultiCoordinate extends GridCoordinate {

    private final Iterable<Item> items;
    
    protected GridMultiCoordinate(Iterable<Item> items, String id) {
        super(id);
        this.items = items;
    }
    
    public Iterable<Item> getItems() {
        return items;
    }
    
}
