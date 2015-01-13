package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

import com.google.common.base.Objects;

class TableCoordinate {

    private final String id;
    private final Item item;
    
    protected TableCoordinate(String id, Item item) {
        this.id = id;
        this.item = item;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
      
        return Objects.equal(this.id, ((TableCoordinate)other).id)
                && Objects.equal(this.item, ((TableCoordinate)other).item)
        ;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id, item);
    }
    
    static TableCoordinate from(GridSimpleCoordinate coordinate) {
        return new TableCoordinate(coordinate.getId(), null);
    }
    
    static TableCoordinate from(GridSingleCoordinate coordinate) {
        return new TableCoordinate(coordinate.getId(), coordinate.getItem());
    }
    
    static TableCoordinate from(GridMultiCoordinate coordinate, Item item) {
        return new TableCoordinate(coordinate.getId(), item);
    }
    
}
