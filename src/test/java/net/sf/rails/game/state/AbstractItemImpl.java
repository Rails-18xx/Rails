package net.sf.rails.game.state;

import net.sf.rails.game.state.AbstractItem;
import net.sf.rails.game.state.Item;

// Implementation for Testing only
class AbstractItemImpl extends AbstractItem {
    AbstractItemImpl(Item parent, String id) {
        super(parent, id);
    }
    
    static AbstractItemImpl create(Item parent, String id) {
        return new AbstractItemImpl(parent, id);
    }
    
    public String toString() {
        return "Item(Id=" + getId() + ")";
    }
}
