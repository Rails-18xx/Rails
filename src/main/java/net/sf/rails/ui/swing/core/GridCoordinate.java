package net.sf.rails.ui.swing.core;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import net.sf.rails.game.state.Item;

/**
 * Abstract base class for coordinates used in GridTables
 */
public abstract class GridCoordinate {

    private final String id;
    private boolean visible;

    protected GridCoordinate(String id) {
        this.id = id;
        this.visible = true;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public GridCoordinate show() {
        visible = true;
        return this;
    }
    
    public GridCoordinate hide() {
        visible = false;
        return this;
    }
    
    public abstract ImmutableSet<TableCoordinate> toTableCoordinates();
    
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        
        return Objects.equal(this.id, ((GridCoordinate)other).id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    public static GridSimpleCoordinate from(String id) {
        return new GridSimpleCoordinate(id);
    }

    public static GridSimpleCoordinate from(Enum<?> e) {
        return new GridSimpleCoordinate(e.name());
    }
    
    public static GridSingleCoordinate from(Item item) {
        return new GridSingleCoordinate(item, item.getFullURI());
    }
    
    public static <T extends Item> GridMultiCoordinate from(Iterable<T> items, Class<T> clazz) {
        StringBuilder id = new StringBuilder();
        for (Item item:items) {
            id.append(item.getFullURI());
        }
        return new GridMultiCoordinate(items, clazz, id.toString());
    }
    
}
