package net.sf.rails.ui.swing.core;

import java.awt.Color;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

import com.google.common.collect.ImmutableTable;

/**
 * GridTable generates a table for UI displays
 * based on Accessors
 */

public class GridTable {
    
    public static enum TYPE {HEADER, INNER}
    
    private final ImmutableTable<GridCoordinate, GridCoordinate, GridField> fields;
    private final GridAxis rows;
    private final GridAxis cols;
    
    private static Logger log = LoggerFactory.getLogger(GridTable.class);

    private GridTable(Builder builder) {
        this.fields = builder.fields.build();
        this.rows = builder.rows;
        this.cols = builder.cols;
    }
    
    ImmutableTable<GridCoordinate, GridCoordinate, GridField> getFields() {
        return fields;
    }
    
    GridAxis getRows() {
        return rows;
    }
    
    GridAxis getCols() {
        return cols;
    }
    
    public static Builder builder(GridAxis rows, GridAxis cols) {
        return new Builder(rows, cols);
    }
    
    public static class Builder {

        private final ImmutableTable.Builder<GridCoordinate, GridCoordinate, GridField> fields =
                ImmutableTable.builder();
        
        private final GridAxis rows;
        private final GridAxis cols;
        
        private GridFieldFormat headerFormat = GridFieldFormat.builder().build();
        private GridFieldFormat innerFormat = GridFieldFormat.builder().setBackground(Color.WHITE).build();
        
        private final Iterator<GridCoordinate> rowIterator;
        private Iterator<GridCoordinate> colIterator;
        private GridCoordinate currentRow;
        private GridField currentField;
        
        private Builder(GridAxis rows, GridAxis cols) {
            this.rows = rows;
            this.cols = cols;
            rowIterator = rows.iterator();
        }
        
        public Builder setHeaderFormat(GridFieldFormat format) {
            this.headerFormat = format;
            return this;
        }
 
        public Builder setInnerFormat(GridFieldFormat format) {
            this.innerFormat = format;
            return this;
        }
 
        public Builder row() {
            currentRow = rowIterator.next();
            colIterator = cols.iterator();
            return this;
        }
        
        private Builder addField(GridField field, GridFieldFormat format) {
            field.setFormat(format);
            currentField = field;
            GridCoordinate currentCol = colIterator.next();
            fields.put(currentRow, currentCol, currentField);
            log.debug("Add field at {},{} ", currentRow, currentCol);
            return this;
        }
        
        public Builder add(String text) {
            return addField(GridField.createFrom(text), headerFormat);
        }
        
        public Builder add(Observable observable) {
            return addField(GridField.createFrom(observable), headerFormat);
        }
        
        public Builder add(Accessor1D<? extends Item> accessor) {
            return addField(GridField.createFrom(accessor), innerFormat);
        }

        public Builder add(Accessor2D<? extends Item,? extends Item> accessor) {
            return addField(GridField.createFrom(accessor), innerFormat);
        }
        
        public Builder innerFormat() {
            currentField.setFormat(innerFormat);
            return this;
        }
        
        public Builder headerFormat() {
            currentField.setFormat(headerFormat);
            return this;
        }

        public Builder color(Accessor1D.AColorModel<? extends Item> color) {
            if (currentField instanceof GridFieldDynamic1D) {
                ((GridFieldDynamic1D)currentField).setColor(color);
            }
            return this;
        }

        
        public GridTable build() {
            return new GridTable(this);
        }
        
    }
        
}
