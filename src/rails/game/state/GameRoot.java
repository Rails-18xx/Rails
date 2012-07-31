package rails.game.state;
/**
 * A context describe a service that allows to locate items
 * 
 * TODO: Check if we should check for non-null id here
 */
public abstract class GameRoot implements Item {

    /**
     * @param Either a fullURI or a (relative) URI inside the Context 
     * @return Item if found, otherwise null
     */
    public abstract Item locate(String uri);

    abstract void addItem(Item item);

    abstract void removeItem(Item item);
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(Id=" + getId() + ")";
    }
}
