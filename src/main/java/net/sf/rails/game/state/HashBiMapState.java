package net.sf.rails.game.state;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Stateful version of a BiMap
 */
public class HashBiMapState<K,V> extends MapState<K,V> {
    
    private final HashBiMap<K,V> biMap;
    
    private HashBiMapState(Item parent, String id, Map<K,V> map) {
        super(parent, id);
        if (map == null) {
            this.biMap = HashBiMap.create();
        } else {
            this.biMap = HashBiMap.create(map);
        }
    }

    /**
     * creates an empty BiMapState
     * @return empty BiMapState
     */
    public static <K,V> HashBiMapState<K, V> create(Item parent, String id){
        return new HashBiMapState<K,V>(parent, id, null);
    }

    /**
     * creates an initialized (filled) BiMapState
     * @param map used for initialization
     * @return initialized BiMapState
     */
    public static <K,V> HashBiMapState<K, V> create(Item parent, String id, Map<K,V> map){
        return new HashBiMapState<K,V>(parent, id, map);
    }

    /**
     * Add key,value pair to Bimap
     * @param key for mapping
     * @param value associated with key 
     * @return previous value associated with specified key, or null if there was no mapping for the key (or null was the value).
     */
    @Override
    public V put(K key, V value) {
        // check if the key is in the map
        if (biMap.containsKey(key)) {
            V oldValue = biMap.get(key);
            // check if element already has the specified value
            if (!oldValue.equals(value)) {
                new MapChange<K,V>(this, key, value);
            }
            return oldValue;
        } else {
            // if not in biMap, add tuple and return null
            new MapChange<K,V>(this, key, value);
            return null;
        }
    }
    
    /**
     * Adds all (key,value) pairs
     * @param map that gets added
     * @throws NullPointerException if map is null
     */
    @Override
    public void putAll(Map<K,V> map) {
        checkNotNull(map);
        for (K key:map.keySet()) {
            put(key, map.get(key));
        }
    }
    
    /**
     * return value for specified key
     * @param key used to retrieve value
     * @return value associated with the key, null if map does not contain key
     */
    @Override
    public V get(K key) {
        return biMap.get(key);
    }

    /**
     * removes key from mapping
     * @param key to be removed from map
     * @return value previously associated with key, null if map did not contain key
     */
    @Override
    public V remove(K key) {
        // check if map contains key
        if (!biMap.containsKey(key)) return null;
        V old = biMap.get(key);
        new MapChange<K,V>(this, key);
        return old;
    }

    /**
     * test if key is present in mapping
     * @param key whose presence is tested
     * @return true if key is present
     */
    @Override
    public boolean containsKey(K key) {
        return biMap.containsKey(key);
    }

    /**
     * removes all mappings from the map
     */
    @Override
    public void clear() {
        for (K key : ImmutableSet.copyOf(biMap.keySet())) {
            remove (key);
        }
    }
    
    /**
     * checks if biMap is empty
     * @return true if biMap is empty
     */
    public boolean isEmpty() {
        return biMap.isEmpty();
    }
    
    /**
     * @return number of elements
     */
    public int size() {
        return biMap.size();
    }

    /**
     * (re)initializes the state biMap from another biMap
     * @param biMap used for initialization
     */
    public void initFromMap(Map<K,V> initMap) {
        // all from initMap get added
        putAll(initMap);
        // remove those only in current biMap
        for (K key:ImmutableSet.copyOf(Sets.difference(biMap.keySet(), initMap.keySet()))) {
            remove(key);
        }
    }

    /**
     * creates an immutable copy of the biMap
     * @return immutable version of the biMap
     */
    public ImmutableBiMap<K,V> viewMap() {
        return ImmutableBiMap.copyOf(biMap);
    }

    /**
     * creates an immutable copy of the keyset
     * @return immutable keyset of the biMap
     */
    public ImmutableSet<K> viewKeySet() {
        return ImmutableSet.copyOf(biMap.keySet());
    }

    /**
     * creates an immutable copy of the values
     * @return immutable list of values
     */
    public ImmutableSet<V> viewValues() {
        return ImmutableSet.copyOf(biMap.values());
    }

    
    public Iterator<V> iterator() {
        return ImmutableSet.copyOf(biMap.values()).iterator();
    }


    @Override
    public String toText() {
        return biMap.toString();
    }
    
    void change(K key, V value, boolean remove) {
        if (remove) {
            biMap.remove(key);
        } else {
            biMap.put(key, value);
        }
    }
}
