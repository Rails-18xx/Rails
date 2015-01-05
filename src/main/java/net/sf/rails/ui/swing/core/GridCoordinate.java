package net.sf.rails.ui.swing.core;

import com.google.common.base.Objects;

import net.sf.rails.game.state.Item;

/**
 * Abstract base class for coordinates used in GridTables
 */
public class GridCoordinate {

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
    
    public GridCoordinate setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public GridCoordinate show() {
        return setVisible(true);
    }
    
    public GridCoordinate hide() {
        return setVisible(false);
    }
    
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
    
    public static GridSimpleCoordinate from(Object column) {
        return new GridSimpleCoordinate(column.toString());
    }
   
    public static GridSingleCoordinate from(Item item) {
        return new GridSingleCoordinate(item, item.getFullURI());
    }

    public static GridMultiCoordinate from(Iterable<Item> items) {
        StringBuilder id = new StringBuilder();
        for (Item item:items) {
            id.append(item.getFullURI());
        }
        return new GridMultiCoordinate(items, id.toString());
    }
    
}
