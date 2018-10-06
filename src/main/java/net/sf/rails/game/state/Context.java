package net.sf.rails.game.state;

import com.google.common.base.MoreObjects;

/**
 * A context describe a service that allows to locate items
 * <p>
 * TODO: Check if we should check for non-null id here
 */
public abstract class Context implements Item {

    /**
     * @param uri Either a fullURI or a (relative) URI inside the Context
     * @return Item if found, otherwise null
     */
    public abstract Item locate(String uri);

    abstract void addItem(Item item);

    abstract void removeItem(Item item);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .toString();
    }
}
