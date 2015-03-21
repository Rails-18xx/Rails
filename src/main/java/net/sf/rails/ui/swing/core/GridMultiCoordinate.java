package net.sf.rails.ui.swing.core;

import com.google.common.collect.ImmutableSet;

import net.sf.rails.game.state.Item;

/**
 * A GridMultiCoordinate is built from a list/iterable of items
 */
public class GridMultiCoordinate extends GridCoordinate {

    private final Iterable<? extends Item> items;
    private final Class<? extends Item> clazz;
    
    GridMultiCoordinate(Iterable<? extends Item> items, Class<? extends Item> clazz) {
        super(clazz.getName());
        this.items = items;
        this.clazz = clazz;
    }
    
    public Class<? extends Item> getItemClass() {
        return clazz;
    }
    
    public Iterable<? extends Item> getItems() {
        return items;
    }

    @Override
    public ImmutableSet<TableCoordinate> toTableCoordinates() {
        ImmutableSet.Builder<TableCoordinate> coordinates = ImmutableSet.builder();
        for (Item item:items) {
            coordinates.add(TableCoordinate.from(this, item));
        }
        return coordinates.build();
    }
}
