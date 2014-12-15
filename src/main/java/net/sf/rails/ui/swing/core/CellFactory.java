package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Accessor;
import net.sf.rails.game.state.Item;

/**
 * A CellModelFactory is defined by an accessor
 * It then takes a RailsItem and returns the observable required
 */

public abstract class CellFactory<R extends Item>  {

    private final Accessor<R> accessor;
    
    public CellFactory(Accessor<R> accessor) {
        this.accessor = accessor;
    }
    
    public Item getCellElement(R parent) {
        return accessor.access(parent);
    }
    
}
