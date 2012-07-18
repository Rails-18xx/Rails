package rails.game.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;

/**
 * A stateful version of an ArrayList
 * TODO: Add all methods of List interface
 */
public final class ArrayListState<E> extends State implements Iterable<E>  {

    private final ArrayList<E> list;

    private ArrayListState(Item parent, String id, Collection<E> collection) {
        super(parent, id);
        if (collection == null) list = new ArrayList<E>();
        else list = new ArrayList<E>(collection);
    }

    /** 
     * Creates empty ArrayListState 
     */
    public static <E> ArrayListState<E> create(Item parent, String id){
        return new ArrayListState<E>(parent, id, null);
    }
    
    /**
     * Creates a prefilled ArrayListState
     */
    public static <E> ArrayListState<E> create(Item parent, String id, Collection<E> collection){
        return new ArrayListState<E>(parent, id, collection);
    }
    
    /**
     * Appends the specified element to the end of the list
     * @param element to be appended
     * @return true (similar to the general contract of Collection.add)
     */
    public boolean add(E element) {
        new ArrayListChange<E>(this, element, list.size());
        return true;
    }

    /**
     * Inserts specified element at the specified position.
     * @param element to be added
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void add(int index, E element) {
        if (index < 0 || index > list.size()) throw new IndexOutOfBoundsException();
        // if bounds ok, generate change
        new ArrayListChange<E>(this, element, index);
    }

    public boolean remove(E element) {
        if (list.contains(element)) {
            new ArrayListChange<E>(this, list.indexOf(element));
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * remove element at index position
     * @param index position
     * @return element removed
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public E remove(int index) {
        if (index < 0 || index > list.size()) throw new IndexOutOfBoundsException();
        E element = list.get(index);
        // if bounds ok, generate change
        new ArrayListChange<E>(this, index);
        return element;
    }
    
    /**
     * move element to a new index position in the list
     * Remark: index position relative to the list after removal of the element
     * @param element the specified element 
     * @param index of the new position
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
    
    public boolean contains (E element) {
        return list.contains(element);
    }

    public void clear() {
        for (E element:ImmutableList.copyOf(list)) {
            remove(element);
        }
    }

    /**
     * creates an immutable view of the list
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
     * @return a suitable iterator for ArrayListState
     */
    public Iterator<E> iterator() {
        return ImmutableList.copyOf(list).iterator();
    }
    
    @Override
    public String observerText() {
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
