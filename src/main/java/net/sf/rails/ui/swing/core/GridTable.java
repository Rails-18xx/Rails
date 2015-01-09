package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

import com.google.common.collect.ImmutableTable;

/**
 * GridTable generates a table for UI displays
 * based on Accessors
 */

public class GridTable {
    
    private final ImmutableTable<GridCoordinate, GridCoordinate, GridField> fields;
    private final GridAxis rows;
    private final GridAxis cols;
    
    private GridTable(Builder builder) {
        this.fields = builder.fields.build();
        this.rows = builder.rows;
        this.cols = builder.cols;
    }
    
    public static Builder builder(GridAxis rows, GridAxis cols) {
        return new Builder(rows, cols);
    }
    
    public static class Builder {

        private final ImmutableTable.Builder<GridCoordinate, GridCoordinate, GridField> fields =
                ImmutableTable.builder();
        
        private final GridAxis rows;
        private final GridAxis cols;
        
        private GridCoordinate currentRow;
        private GridCoordinate currentCol;
        private GridField currentField;
        
        private Builder(GridAxis rows, GridAxis cols) {
            this.rows = rows;
            this.cols = cols;
        }

        public Builder row() {
            if (currentRow == null) {
                currentRow = rows.first();
            } else {
                currentRow = rows.next();
            }
            currentCol = null; 
            return this;
        }
        
        private Builder addField(GridField field) {
            if (currentCol == null) {
                currentCol = cols.first();
            } else {
                currentCol = cols.next();
            }
            currentField = field;
            fields.put(currentRow, currentCol, currentField);
            return this;
        }
        
        public Builder add(String text) {
            return addField(GridField.createFrom(text));
        }
        
        public Builder add(Observable text) {
            return addField(GridField.createFrom(text));
        }
        
        public Builder add(Accessor1D<? extends Item> text) {
            return addField(GridField.createFrom(text));
        }

        public Builder add(Accessor2D<? extends Item,? extends Item> text) {
            return addField(GridField.createFrom(text));
        }
        
        public GridTable build() {
            return new GridTable(this);
        }
        
    }
        
}
