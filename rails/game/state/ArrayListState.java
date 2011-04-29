package rails.game.state;

import java.util.*;

import rails.game.move.AddToList;
import rails.game.move.RemoveFromList;

/**
 * State class that wraps an ArrayList
 * Generates according list moves
 *
 * Remark: Does not extend State or implements StateI do avoid additional overhead
 * All state/move mechanisms already contained in Move objects
 * For the future a simpler unified StateI would make things clearer
 *
 * TODO: Replace all stateful lists by this class and simplify according move objects
 *
 */

public class ArrayListState<E>  {

    private final ArrayList<E> list = new ArrayList<E>();
    private String listName;

    /**
     * constructor for an empty list
     * @param name
     */
    public ArrayListState(String listName) {
        this.listName = listName;
    }
    /**
     * constructor for a prefilled list
     * @param element
     */
    public ArrayListState(String listName, Collection<E> collection) {
        this(listName);
        list.addAll(collection);
    }

    public void add(E element) {
        new AddToList<E>(list, element, listName);
    }

    public void add(int index, E element) {
        new AddToList<E>(list, element, index, listName);
    }

    public boolean remove(E element) {
        if (list.contains(element)) {
            new RemoveFromList<E>(list, element, listName);
            return true;
        } else {
            return false;
        }
    }

    public void move (E element, int toIndex) {
        if (remove (element)) add (toIndex, element);
    }
    
    public boolean contains (E element) {
        return list.contains(element);
    }

    public void clear() {
        for (E element:list) {
            remove(element);
        }
    }

    /**
     * returns unmodifiable view of list
     */
    public List<E> viewList() {
        return Collections.unmodifiableList(list);
    }

    public int size() {
        return list.size();
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public E get(int index) {
        return list.get(index);
    }
}
