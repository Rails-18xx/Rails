package net.sf.rails.ui.swing.core;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.Observable;

/**
 * A CellModelFactory is defined by an accessor
 * It then takes a RailsItem and returns the observable required
 */

public abstract class CellObservableFactory<R extends RailsItem, O extends Observable>  {

    private final Accessor<R,O> accessor;
    
    public CellObservableFactory(Accessor<R,O> accessor) {
        this.accessor = accessor;
    }
    
    public Observable getCellObservable(R parent) {
        return accessor.access(parent);
    }
    
}
