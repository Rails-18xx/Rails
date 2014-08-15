package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * MapState is an abstract parent class for both HashMapState and HashBiMapState 
 */

public abstract class MapState<K,V> extends State implements Iterable<V> {

    protected MapState(Item parent, String id) {
        super(parent, id);
    }
    
    protected abstract Map<K,V> getMap();

    /**
     * Add key,value pair to getMap()
     * @param key for mapping
     * @param value associated with key 
     * @return previous value associated with specified key, or null if there was no mapping for the key (or null was the value).
     */
    public V put(K key, V value) {
        // check if the key is in the getMap()
        if (getMap().containsKey(key)) {
            V oldValue = getMap().get(key);
            // check if element already has the specified value
            if (!oldValue.equals(value)) {
                new MapChange<K,V>(this, key, value);
            }
            return oldValue;
        } else {
            // if not in getMap(), add tuple and return null
            new MapChange<K,V>(this, key, value);
            return null;
        }
    }

    /**
     * Adds all (key,value) pairs
     * @param getMap() that gets added
     * @throws NullPointerException if getMap() is null
     */
    public void putAll(Map<K,V> map) {
        checkNotNull(map);
        for (K key:map.keySet()) {
            put(key, map.get(key));
        }
    }
    
    /**
     * return value for specified key
     * @param key used to retrieve value
     * @return value associated with the key, null if getMap() does not contain key
     */
    public V get(K key) {
        return getMap().get(key);
    }

    /**
     * removes key from mapping
     * @param key to be removed from getMap()
     * @return value previously associated with key, null if getMap() did not contain key
     */
    public V remove(K key) {
        // check if getMap() contains key
        if (!getMap().containsKey(key)) return null;
        V old = getMap().get(key);
        new MapChange<K,V>(this, key);
        return old;
    }

    /**
     * test if key is present in mapping
     * @param key whose presence is tested
     * @return true if key is present
     */
    public boolean containsKey(K key) {
        return getMap().containsKey(key);
    }
    
    /**
     * removes all mappings from the getMap()
     */
    public void clear() {
        for (K key : ImmutableSet.copyOf(getMap().keySet())) {
            remove (key);
        }
    }
    
    /**
     * checks if getMap() is empty
     * @return true if getMap() is empty
     */
    public boolean isEmpty() {
        return getMap().isEmpty();
    }
    
    /**
     * @return number of elements
     */
    public int size() {
        return getMap().size();
    }

    /**
     * (re)initializes the state getMap() from another getMap()
     * @param getMap() used for initialization
     */
    public void initFromMap(Map<K,V> initMap) {
        // all from initMap get added
        putAll(initMap);
        // remove those only in current map
        for (K key:ImmutableSet.copyOf(Sets.difference(getMap().keySet(), initMap.keySet()))) {
            remove(key);
        }
    }
    
    /**
     * creates an immutable copy of the getMap()
     * @return immutable version of the getMap()
     */
    public ImmutableMap<K,V> view() {
        return ImmutableMap.copyOf(getMap());
    }
    
    /**
     * creates an immutable copy of the keyset
     * @return immutable keyset of the getMap()
     */
    public ImmutableSet<K> viewKeySet() {
        return ImmutableSet.copyOf(getMap().keySet());
    }
    
    public abstract ImmutableCollection<V> viewValues();

    public Iterator<V> iterator() {
        return viewValues().iterator();
    }
    
    void change(K key, V value, boolean remove) {
        if (remove) {
            getMap().remove(key);
        } else {
            getMap().put(key, value);
        }
    }
    
    @Override
    public String toText() {
        return getMap().toString();
    }
}
