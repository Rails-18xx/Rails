package net.sf.rails.ui.swing.core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class TableUI extends ItemUI {
    
    private final Table<TableCoordinate, TableCoordinate, TableField> fields =
            HashBasedTable.create();

    private TableAxis rows;
    private TableAxis cols;
    
    private TableUI(ItemUI parent, String id) {
        super(parent, id);
    }
    
    TableUI setRows(TableAxis rows) {
        this.rows = rows;
        return this;
    }
    
    TableUI setCols(TableAxis cols) {
        this.cols = cols;
        return this;
    }
    
    public static TableUI from(ItemUI parent, String id, GridTable gridTable) {
        TableUI table = new TableUI(parent, id);
        table.setRows(TableAxis.from(gridTable.getRows()));
        table.setCols(TableAxis.from(gridTable.getCols()));
        return table;
    }
    

}
