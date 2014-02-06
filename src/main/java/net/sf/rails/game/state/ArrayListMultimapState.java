package net.sf.rails.game.state;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public final class ArrayListMultimapState<K,V> extends State {

    private final ArrayListMultimap<K,V> map = ArrayListMultimap.create(); ;

    private ArrayListMultimapState(Item parent, String id) {
        super(parent, id);
    }
    
    /** 
     * Creates an empty ArrayListMultimapState 
     */
    public static <K,V> ArrayListMultimapState<K,V> create(Item parent, String id){
        return new ArrayListMultimapState<K,V>(parent, id);
    }
   
    /**
     * Stores a key-value pair in the multimap
     * @param key key to store
     * @param value value to store
     * @return true always
     */

    public boolean put(K key, V value) {
        new ArrayListMultimapChange<K,V>(this, key, value, map.get(key).size());
        return true;
    }
    
    public List<V> get(K key) {
        return map.get(key);
    }
    
    public boolean remove(K key, V value) {
        if (!map.containsEntry(key, value)) return false;
        new ArrayListMultimapChange<K,V>(this, key, map.get(key).indexOf(value));
        return true;
    }
    
    public boolean containsEntry(K key, V value) {
        return map.containsEntry(key, value);
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toText() {
        return map.toString();
    }

    void change(K key, V value, int index, boolean addToList) {
        if (addToList) {
            map.get(key).add(index, value);
        } else {
            map.get(key).remove(index);
        }
    }

    
}
