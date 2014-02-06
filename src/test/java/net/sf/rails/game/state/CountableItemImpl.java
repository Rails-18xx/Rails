package net.sf.rails.game.state;

import net.sf.rails.game.state.Countable;
import net.sf.rails.game.state.CountableItem;
import net.sf.rails.game.state.Item;

class CountableItemImpl extends CountableItem<Countable> {
    
    protected CountableItemImpl(Item parent, String id) {
        super(parent, id, Countable.class);
    }

    static CountableItemImpl create(Item parent, String id) {
        return new CountableItemImpl(parent, id);
    }
    
}
