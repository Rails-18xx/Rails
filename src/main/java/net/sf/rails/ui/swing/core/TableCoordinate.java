package net.sf.rails.ui.swing.core;

import com.google.common.base.Objects;

import net.sf.rails.game.state.Item;

/**
 * Abstract base class for coordinates used in GridTables
 */
public class TableCoordinate {

    private final String id;
    private boolean visible;

    protected TableCoordinate(String id) {
        this.id = id;
        this.visible = true;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public TableCoordinate setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public TableCoordinate show() {
        return setVisible(true);
    }
    
    public TableCoordinate hide() {
        return setVisible(false);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        
        return Objects.equal(this.id, ((TableCoordinate)other).id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    public static TableSimpleCoordinate from(Object column) {
        return new TableSimpleCoordinate(column.toString());
    }
   
    public static TableSingleCoordinate from(Item item) {
        return new TableSingleCoordinate(item, item.getFullURI());
    }

    public static TableMultiCoordinate from(Iterable<Item> items) {
        StringBuilder id = new StringBuilder();
        for (Item item:items) {
            id.append(item.getFullURI());
        }
        return new TableMultiCoordinate(items, id.toString());
    }
    
}
