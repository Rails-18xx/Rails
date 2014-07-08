package net.sf.rails.game.state;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * A stateful version of a HashMap
 * 
 * It allows automatic iteration over it values
 */
public final class HashMapState<K,V> extends MapState<K,V> implements Iterable<V> {

    private HashMapState(Item parent, String id, Map<K,V> map) {
        super(parent, id);
        if (map == null) this.map = Maps.newHashMap();
        else this.map = Maps.newHashMap(map);
    }

    /**
     * creates an empty HashMapState
     * @return empty HashMapState
     */
    public static <K,V> HashMapState<K,V> create(Item parent, String id){
        return new HashMapState<K,V>(parent, id, null);
    }

    /**
     * creates an initialized (filled) HashMapState
     * @param map used for initialization
     * @return initialized HashMapState
     */
    public static <K,V> HashMapState<K,V> create(Item parent, String id, Map<K,V> map){
        return new HashMapState<K,V>(parent, id, map);
    }

    /**
     * creates an immutable copy of the values
     * @return immutable list of values
     */
    @Override
    public ImmutableList<V> viewValues() {
        return ImmutableList.copyOf(map.values());
    }
}
