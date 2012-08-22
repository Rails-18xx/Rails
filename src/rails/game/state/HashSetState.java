package rails.game.state;

import java.util.*;

import com.google.common.collect.ImmutableSet;

/**
 * State class that wraps a HashSet
 */
public final class HashSetState<E> extends State implements Iterable<E> {

    private final HashSet<E> set;

    private HashSetState(Item parent, String id, Collection<E> collection) {
        super(parent, id);
        if (collection == null) set = new HashSet<E>();
        else set = new HashSet<E>(collection);
    }

    /**
     * @return empty HashSetState
     */
    public static <E> HashSetState<E> create(Item parent, String id){
        return new HashSetState<E>(parent, id, null);
    }
    
    /**
     * @return prefilled HashSetState
     */
    public static <E> HashSetState<E> create(Item parent, String id, Collection<E> collection){
        return new HashSetState<E>(parent, id, collection);
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
     * removes all elements
     */
    public void clear() {
        for (E element:ImmutableSet.copyOf(set)) {
            remove(element);
        }
    }

    /**
     * @return immutable view of set
     */
    public ImmutableSet<E> view() {
        return ImmutableSet.copyOf(set);
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
    
    public Iterator<E> iterator() {
        return set.iterator();
    }
    
    @Override
    public String toText() {
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
