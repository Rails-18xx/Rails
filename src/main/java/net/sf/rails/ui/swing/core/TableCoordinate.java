package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

import com.google.common.base.Objects;

class TableCoordinate {

    private final GridCoordinate gridCoordinate;
    private final Item item;
    
    private TableCoordinate(GridCoordinate gridCoordinate, Item item) {
        this.gridCoordinate = gridCoordinate;
        this.item = item;
    }
    
    GridCoordinate getGridCoordinate() {
        return gridCoordinate;
    }
    
    Item getItem() {
        return item;
    }
 
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
      
        return Objects.equal(this.gridCoordinate, ((TableCoordinate)other).gridCoordinate)
                && Objects.equal(this.item, ((TableCoordinate)other).item)
        ;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(gridCoordinate, item);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper("T#")
                .addValue(gridCoordinate)
                .addValue(item)
                .omitNullValues()
                .toString();
    }
    
    static TableCoordinate from(GridSimpleCoordinate coordinate) {
        return new TableCoordinate(coordinate, null);
    }
    
    static TableCoordinate from(GridSingleCoordinate coordinate) {
        return new TableCoordinate(coordinate, coordinate.getItem());
    }
    
    static TableCoordinate from(GridMultiCoordinate coordinate, Item item) {
        return new TableCoordinate(coordinate, item);
    }
    
}
