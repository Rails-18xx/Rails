package rails.game.state;

import java.util.*;

import rails.game.move.SetChange;
/**
 * State class that wraps a HashSet
 * Generates according set moves
 *
 * Remark: Does not extend State or implements StateI do avoid additional overhead
 * All state/move mechanisms already contained in Move objects
 *
 * TODO: Replace all stateful sets by this class and simplify according move objects
 */
public class HashSetState<E>  {

    private final HashSet<E> set = new HashSet<E>();
    private final String setName;

    /**
     * constructor for an empty set
     * @param name
     */
    public HashSetState(String setName) {
        this.setName = setName;
    }
    /**
     * constructor for a prefilled set
     * @param element
     */
    public HashSetState(String setName, Collection<E> collection) {
        this(setName);
        set.addAll(collection);
    }

    public void add(E element) {
        new SetChange<E>(set, element, true);
    }

    public boolean remove(E element) {
        if (set.contains(element)) {
            new SetChange<E>(set, element, false);
            return true;
        } else {
            return false;
        }
    }

    public boolean contains (E element) {
    	return set.contains(element);
    }

    public void clear() {
        for (E element:set) {
            remove(element);
        }
    }

    /**
     * returns unmodifiable view of set
     */
    public Set<E> viewSet() {
        return Collections.unmodifiableSet(set);
    }

    public int size() {
        return set.size();
    }

}
