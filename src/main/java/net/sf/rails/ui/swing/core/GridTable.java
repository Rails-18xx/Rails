package net.sf.rails.ui.swing.core;

import com.google.common.collect.ImmutableTable;

/**
 * GridTable generates a table for UI displays
 * based on Accessors
 */

public class GridTable {
    
    private final Fields textFields;
    private final Fields tooltipFields;
    private final Fields colorFields;
     
    private final GridAxis rows;
    private final GridAxis cols;
    
    private GridTable(Builder builder) {
        this.textFields = builder.textFields.build();
        this.tooltipFields = builder.tooltipFields.build();
        this.colorFields = builder.colorFields.build();
        this.rows = builder.rows;
        this.cols = builder.cols;
    }
    
    public static Builder builder(GridAxis rows, GridAxis cols) {
        return new Builder(rows, cols);
    }
    
    private static class Fields {
        
        private final ImmutableTable<GridCoordinate, GridCoordinate, GridField> fields;
        
        private Fields(ImmutableTable<GridCoordinate, GridCoordinate, GridField> fields) {
            this.fields = fields;
        }
        
        private static Fields.Builder builder() {
            return new Fields.Builder();
        }
        
        private static class Builder {
            
            private final ImmutableTable.Builder<GridCoordinate, GridCoordinate, GridField> fields = ImmutableTable.builder();
            
            private Builder() {}
            
            private void put(GridCoordinate row, GridCoordinate col, GridField field) {
                fields.put(row, col, field);
            }
            
            private Fields build() {
                return new Fields(fields.build());
            }
        }
        
    }
    
    public static class Builder {
        
        private final Fields.Builder textFields = Fields.builder();
        private final Fields.Builder tooltipFields = Fields.builder();
        private final Fields.Builder colorFields = Fields.builder();
        
        private final GridAxis rows;
        private final GridAxis cols;
        
        private GridCoordinate currentRow;
        private GridCoordinate currentCol;
        
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
        
        public Builder add(GridField text) {
            if (currentCol == null) {
                currentCol = cols.first();
            } else {
                currentCol = cols.next();
            }
            textFields.put(currentRow, currentCol, text);
            return this;
        }
        
        public Builder toolTip(GridField tooltip) {
            tooltipFields.put(currentRow, currentCol, tooltip);
            return this;
        }
        
        public Builder color(GridField color) {
            colorFields.put(currentRow, currentCol, color);
            return this;
        }
        
        public GridTable build() {
            return new GridTable(this);
        }
        
    }
        
}
