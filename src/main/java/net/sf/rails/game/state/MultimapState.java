package net.sf.rails.game.state;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Abstract class of stateful Multimap
 */
public abstract class MultimapState<K,V> extends State implements Iterable<V> {
    
    protected MultimapState(Item parent, String id) {
        super(parent, id);
    }
    
    // Helper function to return the Multimap from the classes below
    protected abstract Multimap<K,V> getMap();
    
    /**
     * Stores a key-value pair in the multimap
     * @param key key to store
     * @param value value to store
     * @return true if key-value pair is added, or false if the key value pair already exists
     */
    public boolean put(K key, V value) {
        if (getMap().containsEntry(key, value)) return false;
        new MultimapChange<K,V>(this, key, value, true);
        return true;
    }

    public ImmutableSet<V> get(K key) {
        return ImmutableSet.copyOf(getMap().get(key));
    }
    public boolean remove(K key, V value) {
        if (!getMap().containsEntry(key, value)) return false;
        new MultimapChange<K,V>(this, key, value, false);
        return true;
    }
    
    public Set<V> removeAll(K key) {
        Set<V> values = this.get(key);
        for (V value:values) {
            this.remove(key, value);
        }
        return values;
    }
    
    public boolean containsEntry(K key, V value) {
        return getMap().containsEntry(key, value);
    }
    
    public boolean containsKey(K key) {
        return getMap().containsKey(key);
    }

    public boolean containsValue(V value) {
        return getMap().containsValue(value);
    }
    
    public int size() {
        return getMap().size();
    }
    
    public boolean isEmpty() {
        return getMap().isEmpty();
    }
    
    public ImmutableSet<K> keySet() {
        return ImmutableSet.copyOf(getMap().keySet());
    }

    /**
     * @return all values of the multimap
     */
    public ImmutableCollection<V> values() {
        // ImmutableCollection.copyOf does not exist, uses List instead
        return ImmutableList.copyOf(getMap().values());
    }
    
    /**
     * creates an immutable copy of the Multimap
     * @return immutable version of the Multimap
     */
    public ImmutableMultimap<K,V> view() {
        return ImmutableMultimap.copyOf(getMap());
    }
    
    /**
     * @return an iterator over all values
     */
    public Iterator<V> iterator() {
        return ImmutableList.copyOf(getMap().values()).iterator();
    }
    
    @Override
    public String toText() {
        return getMap().toString();
    }
    
    void change(K key, V value, boolean addToMap) {
        if (addToMap) {
            getMap().put(key, value);
        } else {
            getMap().remove(key, value);
        }
    }
}
