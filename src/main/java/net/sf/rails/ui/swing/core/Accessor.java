package net.sf.rails.ui.swing.core;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.Observable;

public interface Accessor<R extends RailsItem, O extends Observable> {

    public Observable access(R parent);
    
}
