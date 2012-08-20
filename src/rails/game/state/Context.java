package rails.game.state;

import com.google.common.base.Objects;

/**
 * A context describe a service that allows to locate items
 * 
 * TODO: Check if we should check for non-null id here
 */
public abstract class Context implements Item {

    /**
     * @param Either a fullURI or a (relative) URI inside the Context 
     * @return Item if found, otherwise null
     */
    public abstract Item locate(String uri);

    abstract void addItem(Item item);

    abstract void removeItem(Item item);
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", getId()).toString();
    }
}
