package rails.game.model;


/**
 * Holder allows the storage of Ownable objects
 * @author freystef
 *
 * @param <E> class of the objects to store
 */
public interface Holder<E extends Ownable> extends Iterable<E>{

    /** Add an object to a holder
     */
    public boolean addObject(E object);

    /** Remove an object from a holder
     */
    public boolean removeObject(E object);

    /**
     * @return Number of objects in the holder
     */
    public int size();
    
    /**
     * @return true if holder is empty
     */
    public boolean isEmpty();
    
    /**
     * @return the owner of the holder
     * TODO: Should be removed as the reference should be from the owner to the holder only
     */
    @Deprecated
    public Owner getOwner();

}
