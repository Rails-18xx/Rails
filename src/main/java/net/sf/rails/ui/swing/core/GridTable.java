package net.sf.rails.ui.swing.core;


import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

/**
 * GridTable generates a table for UI displays
 * based on Accessors
 */

public class GridTable extends ItemUI {
    
    private final Table<TableCoordinate, TableCoordinate, FieldUI> table;
    
    private final TableAxis rows;
    private final TableAxis cols;
    
    private GridTable(ItemUI parent, String id, Table<TableCoordinate, TableCoordinate, FieldUI> table, TableAxis rows, TableAxis cols) {
        super(parent, id);
        this.table = table;
        this.rows = rows;
        this.cols = cols;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final ImmutableTable.Builder<TableCoordinate, TableCoordinate, FieldUI> table= ImmutableTable.builder();
        private final TableAxis.Builder rows = TableAxis.builder();
        private final TableAxis.Builder cols = TableAxis.builder();
        
        private Builder() {}
        
        public Builder addField(TableCoordinate row, TableCoordinate col, FieldUI field) {
            if (!rows.contains(row)) {
                rows.addLast(row);
            }
            if (!cols.contains(col)) {
                cols.addLast(col);
            }
            table.put(row, col, field);
            return this;
        }
        
        public GridTable build(ItemUI parent, String id) {
            return new GridTable(parent, id, table.build(), rows.build(), cols.build());
        }
        
    }
    
        
}
