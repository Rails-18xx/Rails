package rails.game.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rails.game.model.ModelObject;
import rails.game.move.MapChange;
import rails.game.move.RemoveFromMap;
import tools.Util;

/**
 * State class that wraps a HashMap
 * Generates according map moves
 *
 * Remark: Does not extend State or implements StateI do avoid additional overhead
 * All state/move mechanisms already contained in Move objects
 * For the future a simpler unified StateI would make things clearer
 * 
 * TODO: Replace all stateful HashMaps by this class and simplify according move objects
 * 
 */

public class HashMapState<K,V> extends ModelObject {

    private final HashMap<K,V> map = new HashMap<K,V>();
    private String mapName;
    
    /**
     * constructor for an empty map
     */
    public HashMapState(String listName) {
        this.mapName = listName;
    }
    /**
     * constructor for a prefilled map
     */
    public HashMapState(String listName, Map<K,V> map) {
        this(listName);
    }
    
    public void put(K key, V value) {
        new MapChange<K,V>(map, key, value, this);
    }
    
    public void putAll(Map<K,V> map) {
        for (K key:map.keySet()) {
            new MapChange<K,V>(map, key, map.get(key), this);
        }
    }
    
    public V get(K key) {
        return map.get(key);
    }
    
    public void remove(K key) {
       new RemoveFromMap<K,V>(map, key, this);
    }
    
    public boolean hasKey(K key) {
        return map.containsKey(key);
    }
    
    public void clear() {
        // Two-step process to avoid concurrent modification exception
        List<K> keys = new ArrayList<K>();
        for (K key : map.keySet()) {
            keys.add(key);
        }
        for (K key : keys) {
            remove (key);
        }
        update();
    }

    /**
     * (re)intializes the state map from another map
     * efficiently generates the required moves
     */
    public void initFromMap(Map<K,V> initMap) {
        for (K key:map.keySet()) {
            // union elements
            if (initMap.containsKey(key)) {
                new MapChange<K,V>(map, key, initMap.get(key));
            } else { // only in the old map
                new RemoveFromMap<K,V>(map, key);
            }
        }
        for (K key:initMap.keySet()) {
            // new elements
            if (!map.containsKey(key)) {
                new MapChange<K,V>(map, key, initMap.get(key));
            }
        }
        update();
    }
    
    /** 
     * @return unmodifiable view of map
     */
    public Map<K,V> viewMap() {
        return Collections.unmodifiableMap(map);
    }
    /**
     * @return unmodifiable view of keyset
     */
    public Set<K> viewKeySet() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    public Collection<V> viewValues() {
        return Collections.unmodifiableCollection(map.values());
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    @Override
    public String getText() {

        if (map == null) return "";
        
        StringBuilder buf = new StringBuilder("<html>");
        for (K name : map.keySet()) {
            if (buf.length() > 6) buf.append("<br>");
            buf.append(name.toString());
            Object value = map.get(name);
            if (value != null && Util.hasValue(value.toString())) buf.append("=").append(value.toString());
        }
        if (buf.length() > 6) {
            buf.append("</html>");
        }
        return buf.toString();

    }

}
