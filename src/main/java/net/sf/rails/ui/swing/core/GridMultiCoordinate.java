package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * A GridMultiCoordinate is built from a list/iterable of items
 */
public class GridMultiCoordinate extends GridCoordinate {

    private final Iterable<? extends Item> items;
    private final Class<? extends Item> clazz;
    
    protected GridMultiCoordinate(Iterable<? extends Item> items, Class<? extends Item> clazz, String id) {
        super(id);
        this.items = items;
        this.clazz = clazz;
    }
    
    public Class<? extends Item> getItemClass() {
        return clazz;
    }
    
    public Iterable<? extends Item> getItems() {
        return items;
    }
    
}
