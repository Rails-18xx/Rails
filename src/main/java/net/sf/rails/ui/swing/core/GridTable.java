package net.sf.rails.ui.swing.core;


import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

/**
 * GridTable generates a table for UI displays
 * based on Accessors
 */

public class GridTable extends ItemUI {
    
    private final Table<GridCoordinate, GridCoordinate, GridField> table;
    
    private final GridAxis rows;
    private final GridAxis cols;
    
    private GridTable(ItemUI parent, String id, Table<GridCoordinate, GridCoordinate, GridField> table, GridAxis rows, GridAxis cols) {
        super(parent, id);
        this.table = table;
        this.rows = rows;
        this.cols = cols;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final ImmutableTable.Builder<GridCoordinate, GridCoordinate, GridField> table= ImmutableTable.builder();
        private final GridAxis.Builder rows = GridAxis.builder();
        private final GridAxis.Builder cols = GridAxis.builder();
        
        private Builder() {}
        
        public Builder addField(GridCoordinate row, GridCoordinate col, GridField field) {
            if (!rows.contains(row)) {
                rows.add(row);
            }
            if (!cols.contains(col)) {
                cols.add(col);
            }
            table.put(row, col, field);
            return this;
        }
        
        public GridTable build(ItemUI parent, String id) {
            return new GridTable(parent, id, table.build(), rows.build(), cols.build());
        }
        
    }
    
        
}
