package net.sf.rails.game.state;

import java.util.Map;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;

/**
 * Stateful version of a BiMap
 */
public class HashBiMapState<K,V> extends MapState<K,V> {
    
    private HashBiMapState(Item parent, String id, Map<K,V> map) {
        super(parent, id);
        if (map == null) {
            this.map = HashBiMap.create();
        } else {
            this.map = HashBiMap.create(map);
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

    // TODO: Check if value is already in map
    //   public V put(K key, V value) 

    /**
     * creates an immutable copy of the biMap
     * @return immutable version of the biMap
     */
    @Override
    public ImmutableBiMap<K,V> viewMap() {
        return ImmutableBiMap.copyOf(map);
    }

    /**
     * creates an immutable copy of the values
     * @return immutable list of values
     */
    @Override
    public ImmutableSet<V> viewValues() {
        return ImmutableSet.copyOf(map.values());
    }
    
}
