package net.sf.rails.game.state;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A stateful version of an ArrayList
 * TODO: Add all methods of List interface
 */
public final class ArrayListState<E> extends State implements Iterable<E> {

    private final ArrayList<E> list;

    /**
     * Creates a prefilled array list state
     *
     * @param parent     The parent
     * @param id         The id
     * @param collection The content of the state
     */
    public ArrayListState(Item parent, String id, Collection<E> collection) {
        super(parent, id);

        if (collection == null) {
            this.list = new ArrayList<>();
        } else {
            this.list = new ArrayList<>(collection);
        }
    }

    /**
     * Creates an empty array list state
     *
     * @param parent The parent
     * @param id     The id
     */
    public ArrayListState(Item parent, String id) {
        this(parent, id, null);
    }

    /**
     * Appends the specified element to the end of the list
     *
     * @param element to be appended
     * @return true (similar to the general contract of Collection.add)
     */
    public boolean add(E element) {
        new ArrayListChange<E>(this, element, list.size());
        return true;
    }

    /**
     * Inserts specified element at the specified position.
     *
     * @param element to be added
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void add(int index, E element) {
        if (index < 0 || index > list.size()) throw new IndexOutOfBoundsException();
        // if bounds ok, generate change
        new ArrayListChange<>(this, element, index);
    }

    public boolean remove(E element) {
        // check first if element exists
        if (!list.contains(element)) return false;
        new ArrayListChange<>(this, list.indexOf(element));
        return true;
    }

    /**
     * remove element at index position
     *
     * @param index position
     * @return element removed
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public E remove(int index) {
        if (index < 0 || index > list.size()) throw new IndexOutOfBoundsException();
        E element = list.get(index);
        // if bounds ok, generate change
        new ArrayListChange<>(this, index);
        return element;
    }

    /**
     * move element to a new index position in the list
     * Remark: index position relative to the list after removal of the element
     *
     * @param element the specified element
     * @param index   of the new position
     * @return true if the list contained the specified element
     * @throws IndexOutOfBoundsException if the new index is out of range (0 <= index < size)
     */
    public boolean move(E element, int index) {
        if (index < 0 || index > list.size() - 1) throw new IndexOutOfBoundsException();
        // if bounds ok, start move
        boolean remove = remove(element);
        if (remove) { // only if element exists, execute move
            add(index, element);
        }
        return remove;
    }

    public boolean contains(E element) {
        return list.contains(element);
    }

    /**
     * removes all elements
     */
    public void clear() {
        for (E element : ImmutableList.copyOf(list)) {
            remove(element);
        }
    }

    /**
     * make the list identical to the argument list
     */
    public void setTo(List<E> newList) {
        int index = 0;
        List<E> copyList = List.copyOf(list);
        for (E element : newList) {
            if (index < copyList.size()) {
                if (element.equals(copyList.get(index))) {
                    // elements are equal, no change required
                    index++;
                    continue;
                } else {
                    // elements are unequal, so remove old element
                    new ArrayListChange<>(this, index);
                }
            }
            new ArrayListChange<>(this, element, index);
            index++;
        }
        // remove all remaining elements if original list is larger
        for (; index < copyList.size(); index++) {
            new ArrayListChange<>(this, index);
        }
    }

    /**
     * creates an immutable view of the list
     *
     * @return immutable copy
     */
    public ImmutableList<E> view() {
        return ImmutableList.copyOf(list);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public E get(int index) {
        return list.get(index);
    }

    /**
     * creates an iterator derived from the ImmutableCopy of the ArrayListState
     *
     * @return a suitable iterator for ArrayListState
     */
    @Override
    public Iterator<E> iterator() {
        return List.copyOf(list).iterator();
    }

    @Override
    public String toText() {
        return list.toString();
    }

    void change(E object, int index, boolean addToList) {
        if (addToList) {
            list.add(index, object);
        } else {
            list.remove(index);
        }
    }


}
