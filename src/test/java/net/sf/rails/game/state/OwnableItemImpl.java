package net.sf.rails.game.state;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Ownable;
import net.sf.rails.game.state.OwnableItem;

/**
 * Test implementation of OwnableItem
 */
class OwnableItemImpl extends OwnableItem<Ownable> {

    protected OwnableItemImpl(Item parent, String id) {
        super(parent, id, Ownable.class);
    }

    static OwnableItemImpl create(Item parent, String id) {
        return new OwnableItemImpl(parent, id);
    }
    
}
