package rails.game.state;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class ArrayListMultimapState<K,V> extends MultimapState<K,V> {

    private final ArrayListMultimap<K,V> map;

    private ArrayListMultimapState(String id) {
        super(id);
        map = ArrayListMultimap.create();
    }
    
    /** 
     * Creates an owned and empty ArrayListMultimapState 
     */
    public static <K,V> ArrayListMultimapState<K,V> create(Item parent, String id){
        return new ArrayListMultimapState<K,V>(id).init(parent);
    }
   
    /**
     * Creates an unowned and empty ArrayListMultimapState
     * Remark: Still requires a call to the init-method
     */
    public static <K,V> ArrayListMultimapState<K,V> create(String id){
        return new ArrayListMultimapState<K,V>(id);
    }
    
    @Override
    public ArrayListMultimapState<K,V> init(Item parent){
        super.init(parent);
        return this;
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
