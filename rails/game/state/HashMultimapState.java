package rails.game.state;

import java.util.Set;

import com.google.common.collect.HashMultimap;

/**
 * A stateful version of a HashMultimap
 * 
 * @author freystef
 */

public final class HashMultimapState<K,V> extends AbstractState implements MultimapState<K,V> {
    
    private final HashMultimap<K,V> map;

    /**
     * Creates an empty HashMultimap state variable
     * @param owner object containing state (usually this)
     * @param id id state variable
     */
    public HashMultimapState(Item owner, String id) {
        super(owner, id);
        map = HashMultimap.create();
    }
    /**
     * Stores a key-value pair in the multimap
     * @param key key to store
     * @param value value to store
     * @return true if key-value pair is added, or false if the key value pair already exists
     */
    
    public boolean put(K key, V value) {
        if (map.containsEntry(key, value)) return false;
        new MultimapChange<K,V>(this, key, value, true);
        return true;
    }
    
    public Set<V> get(K key) {
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

    public void change(K key, V value, boolean addToMap) {
        if (addToMap) {
            map.put(key, value);
        } else {
            map.remove(key, value);
        }
    }
    
}
