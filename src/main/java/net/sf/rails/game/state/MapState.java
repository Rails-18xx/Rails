package net.sf.rails.game.state;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * MapState is an abstract parent class for both HashMapState and HashBiMapState 
 */

public abstract class MapState<K,V> extends State implements Iterable<V> {

    protected MapState(Item parent, String id) {
        super(parent, id);
    }

    public abstract V put(K key, V value);

    public abstract void putAll(Map<K,V> map);
    
    public abstract V get(K key);

    public abstract V remove(K key);

    public abstract boolean containsKey(K key);
    
    public abstract void clear();
    
    public abstract boolean isEmpty();
    
    public abstract int size();
    
    public abstract ImmutableMap<K,V> viewMap();
    
    public abstract ImmutableSet<K> viewKeySet();
    
    public abstract ImmutableCollection<V> viewValues();

    public abstract Iterator<V> iterator();
    
    abstract void change(K key, V value, boolean remove);
    
}
