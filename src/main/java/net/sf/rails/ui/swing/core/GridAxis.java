package net.sf.rails.ui.swing.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.rails.game.state.Item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * GridAxis is used as the set of column or rows coordinates for a table
 */
public class GridAxis implements Iterator<GridCoordinate> {

    private static final String PRECON_COORD_CONTAINED = "Coordinate %s already contained in GridAxis";
    private static final String PRECON_COORD_MISSING = "Coordinate %s is missing in GridAxis, but was expected";
    
    private final ImmutableList<GridCoordinate> axis;
    
    private int current = 0;
    
    private GridAxis(List<GridCoordinate> axis) {
        this.axis = ImmutableList.copyOf(axis);
    }
    
    public ImmutableList<GridCoordinate> getAxis() {
        return axis;
    }
    
    public int size() {
        return axis.size();
    }
    
    public GridCoordinate first() {
        current = 0;
        return next();
    }
    
    // Iterator interface
    @Override
    public boolean hasNext() {
        return (current < axis.size());
    }

    @Override
    public GridCoordinate next() {
        return axis.get(current++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
        
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final LinkedList<GridCoordinate> axis = Lists.newLinkedList();
        
        private Builder() {}
    
        public Builder add(GridCoordinate coordinate) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            axis.addLast(coordinate);
            return this;
        }
        
        public Builder add(String id) {
            axis.add(GridCoordinate.from(id));
            return this;
        }
        
        public Builder add(Enum<?> e) {
            axis.add(GridCoordinate.from(e));
            return this;
        }
        
        public Builder add(Item item) {
            axis.add(GridCoordinate.from(item));
            return this;
        }
        
        public <T extends Item> Builder add(Iterable<T> items, Class<T> clazz) {
            axis.add(GridMultiCoordinate.from(items, clazz));
            return this;
        }

        public Builder addFirst(GridCoordinate coordinate) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            axis.addFirst(coordinate);
            return this;
        }

        public Builder addBefore(GridCoordinate coordinate, GridCoordinate before) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            Preconditions.checkArgument(axis.contains(before), PRECON_COORD_MISSING, before);
            int index = axis.indexOf(before);
            axis.add(index, coordinate);
            return this;
        }

        public Builder addAfter(GridCoordinate coordinate, GridCoordinate after) {
            Preconditions.checkArgument(!axis.contains(coordinate), PRECON_COORD_CONTAINED, coordinate);
            Preconditions.checkArgument(axis.contains(after), PRECON_COORD_MISSING, after);
            int index = axis.indexOf(after);
            axis.add(index + 1, coordinate);
            return this;
        }
        
        public boolean contains(GridCoordinate coordinate) {
            return axis.contains(coordinate);
        }
        
        public GridAxis build() {
            return new GridAxis(axis);
        }
        
    }

}
