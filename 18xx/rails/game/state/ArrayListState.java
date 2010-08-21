package rails.game.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rails.game.move.AddToList;
import rails.game.move.RemoveFromList;

/**
 * State class that wraps an ArrayList
 * Generates according list moves
 * 
 * @author freystef
 *
 */

public class ArrayListState<E> extends State {
    
    private final ArrayList<E> list = new ArrayList<E>();
    /**
     * constructor for an empty list
     * @param name
     */
    public ArrayListState(String name) {
        super(name, ArrayList.class);
    }
    /**
     * constructor for a prefilled list
     * @param element
     */
    public ArrayListState(String name, Collection<E> collection) {
        super(name, ArrayList.class);
        for (E element:collection) {
            add(element);
        }
    }
    
    public void add(E element) {
        new AddToList<E>(list, element, name);
    }
    
    public void add(int index, E element) {
        new AddToList<E>(list, element, name).atIndex(index);
    }
    
    public void remove(E element) {
        new RemoveFromList<E>(list, element, name);
    }
    
    public void clear() {
        for (E element:list) {
            new RemoveFromList<E>(list, element, name);
        }
    }
    
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
