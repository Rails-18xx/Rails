package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * A GridSingleCoordinate is a coordinate built from one Item
 */
public class GridSingleCoordinate extends GridCoordinate {
    
    private final Item item;

    protected GridSingleCoordinate(Item item, String id) {
        super(id);
        this.item = item;
    }
    
    public Item getItem() {
        return item;
    }

}
 