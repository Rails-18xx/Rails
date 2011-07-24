package rails.game.state;

import java.util.*;

/**
 * State class that wraps a HashSet
 */
public final class HashSetState<E> extends AbstractState {

    private final HashSet<E> set;

    /**
     * Creates an empty HashSet state variable
     * @param owner object containing state (usually this)
     * @param id id state variable
     */
    public HashSetState(Item owner, String id) {
        super(owner, id);
        set = new HashSet<E>();
    }

    /**
     * @param owner object containing state (usually this)
     * @param id id state variable
     * @param collection elements contained in the set at initialization
     */
    public HashSetState(Item owner, String id, Collection<E> collection) {
        super(owner, id);
        set = new HashSet<E>(collection);
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
