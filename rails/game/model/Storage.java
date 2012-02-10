package rails.game.model;


/**
 * Storage keeps several Ownable objects
 * @author freystef
 *
 * @param <E> class of the objects to store
 */
@Deprecated
public interface Storage<E extends Ownable> extends Iterable<E>{

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
     * TODO: Should be removed as the reference should be from the owner to the storage only
     */
    @Deprecated
    public Owner getOwner();

}
