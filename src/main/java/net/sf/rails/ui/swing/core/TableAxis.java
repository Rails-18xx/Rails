package net.sf.rails.ui.swing.core;

import java.util.LinkedList;
import java.util.List;

import net.sf.rails.game.state.Item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * TableAxis is used as the set of column or rows coordinates for a table
 */
public class TableAxis {

    private static final String PRECON_COORD_CONTAINED = "Coordinate %s already contained in TableAxis";
    private static final String PRECON_COORD_MISSING = "Coordinate %s is missing in TableAxis, but was expected";
    
    private final ImmutableList<TableCoordinate> axis;
    
    private TableAxis(List<TableCoordinate> axis) {
        this.axis = ImmutableList.copyOf(axis);
    }
    
    public ImmutableList<TableCoordinate> getAxis() {
        return axis;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final LinkedList<TableCoordinate> axis = Lists.newLinkedList();
        
        private Builder() {}
    
        public Builder add(TableCoordinate coordinate) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            axis.addLast(coordinate);
            return this;
        }
        
        public Builder add(Object column) {
            axis.add(TableCoordinate.from(column));
            return this;
        }
         
        public <R extends Item> Builder add(Iterable<R> items) {
            axis.add(TableMultiCoordinate.from(items));
            return this;
        }

        public Builder addFirst(TableCoordinate coordinate) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            axis.addFirst(coordinate);
            return this;
        }

        public Builder addBefore(TableCoordinate coordinate, TableCoordinate before) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            Preconditions.checkArgument(axis.contains(before), PRECON_COORD_MISSING, before);
            int index = axis.indexOf(before);
            axis.add(index, coordinate);
            return this;
        }

        public Builder addAfter(TableCoordinate coordinate, TableCoordinate after) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            Preconditions.checkArgument(axis.contains(after), PRECON_COORD_MISSING, after);
            int index = axis.indexOf(after);
            axis.add(index + 1, coordinate);
            return this;
        }
        
        public boolean contains(TableCoordinate coordinate) {
            return axis.contains(coordinate);
        }
        
        public TableAxis build() {
            return new TableAxis(axis);
        }
        
    }

}
