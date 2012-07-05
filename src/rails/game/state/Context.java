package rails.game.state;
/**
 * A context describe a service that allows to locate items
 */
public abstract class Context implements Item {

    /**
     * @param Either a fullURI or a (relative) URI inside the Context 
     * @return Item if found, otherwise null
     */
    public abstract Item locate(String uri);

    abstract void addItem(Item item);

    abstract void removeItem(Item item);
    
}
