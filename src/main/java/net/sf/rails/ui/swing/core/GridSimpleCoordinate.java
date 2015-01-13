package net.sf.rails.ui.swing.core;

import com.google.common.collect.ImmutableSet;

/**
 * GridSimpleCoordinate is a named coordinate
 */
public class GridSimpleCoordinate extends GridCoordinate {
    
    protected GridSimpleCoordinate(String id) {
        super(id);
    }

    @Override
    public ImmutableSet<TableCoordinate> toTableCoordinates() {
        return ImmutableSet.of(TableCoordinate.from(this));
    }
    
}
