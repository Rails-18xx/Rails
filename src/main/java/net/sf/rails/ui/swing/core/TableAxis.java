package net.sf.rails.ui.swing.core;

import com.google.common.collect.ImmutableSet;

class TableAxis {

    private final ImmutableSet<TableCoordinate> coordinates;
    
    private TableAxis(ImmutableSet<TableCoordinate> axis){
        this.coordinates = axis;
    }
    
    ImmutableSet<TableCoordinate> getCoordinates() {
        return coordinates;
    }
    
    static TableAxis from(GridAxis gridAxis) {
        ImmutableSet.Builder<TableCoordinate> coordinates = ImmutableSet.builder();
        
        for (GridCoordinate coordinate:gridAxis) {
            coordinates.addAll(coordinate.toTableCoordinates());
        }
        
        return new TableAxis(coordinates.build());
    }
}
 