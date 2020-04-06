package net.sf.rails.ui.swing.core;

import javax.swing.JPanel;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.IRow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class TableUI {

    private final TableAxis rows;
    private final TableAxis cols;
    private final Table<TableCoordinate,TableCoordinate,TableField> fields;

    private static final Logger log = LoggerFactory.getLogger(TableUI.class);

    private TableUI(TableAxis rows, TableAxis cols, Table<TableCoordinate,TableCoordinate,TableField> fields) {
        this.rows = rows;
        this.cols = cols;
        this.fields = fields;
    }

    public JPanel convertToPanel() {

        JPanel panel = new JPanel();
        DesignGridLayout layout = new DesignGridLayout(panel);

        for (TableCoordinate tableRow:rows) {
            IRow layoutRow =layout.row().grid();
            for (TableCoordinate tableCol:cols) {
                layoutRow.add(fields.get(tableRow, tableCol).getUI());
            }
        }
        return panel;
    }

    public static TableUI from(GridTable gridTable) {

        TableAxis rows = TableAxis.from(gridTable.getRows());
        TableAxis cols = TableAxis.from(gridTable.getCols());

        ImmutableTable.Builder<TableCoordinate,TableCoordinate,TableField> tableFields =
                ImmutableTable.builder();

        for (TableCoordinate row:rows) {
            for (TableCoordinate col:cols) {
                log.debug("Try to add field at {},{} ", row, col);
                GridField gridField = gridTable.getFields().get(row.getGridCoordinate(), col.getGridCoordinate());
                Preconditions.checkState(gridField != null, "No gridField available for row %s, col %s ", row, col);
                TableField tableField = gridField.toTableField(row.getItem(), col.getItem());
                tableFields.put(row, col, tableField);
            }
        }
        TableUI table = new TableUI(rows, cols, tableFields.build());

        return table;
    }

}
