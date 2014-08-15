package net.sf.rails.game.state;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public abstract class SetState<E> extends State implements Iterable<E>  {
    
    protected SetState(Item parent, String id) {
        super(parent, id);
    }
    
    protected abstract Set<E> getSet();
    
    /**
     * add element
     * @param element
     */
    public void add(E element) {
        new SetChange<E>(this, element, true);
    }

    /**
     * remove element
     * @param element to remove
     * @return true = was part of the HashSetState
     */
    public boolean remove(E element) {
        if (getSet().contains(element)) {
            new SetChange<E>(this, element, false);
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
        return getSet().contains(element);
    }

    /**
     * removes all elements
     */
    public void clear() {
        for (E element:ImmutableSet.copyOf(getSet())) {
            remove(element);
        }
    }

    /**
     * @return immutable view of getSet()
     */
    public ImmutableSet<E> view() {
        return ImmutableSet.copyOf(getSet());
    }

    /**
     * @return number of elements in HashSetState
     */
    public int size() {
        return getSet().size();
    }
    
    /**
     * @return true if HashSetState is empty
     */
    public boolean isEmpty() {
        return getSet().isEmpty();
    }
    
    public Iterator<E> iterator() {
        return getSet().iterator();
    }
    
    @Override
    public String toText() {
        return getSet().toString();
    }
    
    void change(E element, boolean addToSet) {
        if (addToSet) {
            getSet().add(element);
        } else {
            getSet().remove(element);
        }
    }

}
