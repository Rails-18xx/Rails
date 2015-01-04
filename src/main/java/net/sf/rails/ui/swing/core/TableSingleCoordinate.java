package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * A TableSingleCoordinate is a coordinate built from one Item
 */
public class TableSingleCoordinate extends TableCoordinate {
    
    private final Item item;

    protected TableSingleCoordinate(Item item, String id) {
        super(id);
        this.item = item;
    }
    
    public Item getItem() {
        return item;
    }

}
 