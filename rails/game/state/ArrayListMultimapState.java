package rails.game.state;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class ArrayListMultimapState<K,V> extends AbstractState implements MultimapState<K,V> {

    private final ArrayListMultimap<K,V> map;

    /**
     * Creates an empty ArrayListMultimap state variable
     * @param owner object containing state (usually this)
     * @param id id state variable
     */
    public ArrayListMultimapState(Item owner, String id) {
        super(owner, id);
        map = ArrayListMultimap.create();
    }
    
    /**
     * Stores a key-value pair in the multimap
     * @param key key to store
     * @param value value to store
     * @return true always
     */

    public boolean put(K key, V value) {
        new MultimapChange<K,V>(this, key, value, true );
        return true;
    }
    
    public List<V> get(K key) {
        return map.get(key);
    }
    
    public boolean remove(K key, V value) {
        if (!map.containsEntry(key, value)) return false;
        new MultimapChange<K,V>(this, key, value, false);
        return true;
    }
    
    public boolean containsEntry(K key, V value) {
        return map.containsEntry(key, value);
    }
    
    @Override
    public String toString() {
        return map.toString();
    }

    public void change(K key, V value, boolean addToMap) {
        if (addToMap) {
            map.put(key, value);
        } else {
            map.remove(key, value);
        }
    }
    
}
