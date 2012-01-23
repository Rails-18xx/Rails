package rails.game.state;

import java.util.*;

/**
 * State class that wraps a HashSet
 */
public final class HashSetState<E> extends State {

    private final HashSet<E> set;

    private HashSetState(String id) {
        super(id);
        set = new HashSet<E>();
    }
    private HashSetState(String id, Collection<E> collection) {
        super(id);
        set = new HashSet<E>(collection);
    }
    
    /** 
     * Creates an owned and empty HashSetState 
     */
    public static <E> HashSetState<E> create(Item parent, String id){
        return new HashSetState<E>(id).init(parent);
    }
    
    /**
     * Creates an owned and prefilled HashSetState
     */
    public static <E> HashSetState<E> create(Item parent, String id, Collection<E> collection){
        return new HashSetState<E>(id, collection).init(parent);
    }
    
    /**
     * Creates an unowned and empty HashSetState
     * Remark: Still requires a call to the init-method
     */
    public static <E> HashSetState<E> create(String id){
        return new HashSetState<E>(id);
    }
    
    @Override
    public HashSetState<E> init(Item parent){
        super.init(parent);
        return this;
    }

    /**
     * add element
     * @param element
     */
    public void add(E element) {
        new HashSetChange<E>(this, element, true);
    }

    /**
     * remove element
     * @param element to remove
     * @return true = was part of the HashSetState
     */
    public boolean remove(E element) {
        if (set.contains(element)) {
            new HashSetChange<E>(this, element, false);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * @param element
     * @return true = element exists in HashSetState
     */
    
    public boolean contains (E element) {
    	return set.contains(element);
    }

    /**
     * remove all elements from HashSetState (creates Moves)
     */
    public void clear() {
        for (E element:set) {
            remove(element);
        }
    }

    /**
     * @return non-stateful unmodifiable view of set
     */
    public Set<E> viewSet() {
        return Collections.unmodifiableSet(set);
    }

    /**
     * @return number of elements in HashSetState
     */
    public int size() {
        return set.size();
    }
    
    /**
     * @return true if HashSetState is empty
     */
    public boolean isEmpty() {
        return set.isEmpty();
    }
    
    @Override
    public String toString() {
        return set.toString();
    }
    
    void change(E element, boolean addToSet) {
        if (addToSet) {
            set.add(element);
        } else {
            set.remove(element);
        }
    }
}
