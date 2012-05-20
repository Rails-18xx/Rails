package rails.game.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * A stateful version of a HashMap
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */
public final class HashMapState<K,V> extends State {

    private final HashMap<K,V> map;

    private HashMapState() {
        map = Maps.newHashMap();
    }

    private HashMapState( Map<K,V> map) {
        this.map = Maps.newHashMap(map);
    }

    /**
     * @return empty HashMapState
     */
    public static <K,V> HashMapState<K,V> create(){
        return new HashMapState<K,V>();
    }

    /**
     * @return prefilled HashMapState
     */
    public static <K,V> HashMapState<K,V> create(Map<K,V> map){
        return new HashMapState<K,V>(map);
    }

    public void put(K key, V value) {
        // check if element already has the specified value
        if (map.containsKey(key) && map.get(key).equals(value)) return;
        new HashMapChange<K,V>(this, key, value);
    }

    public void putAll(Map<K,V> map) {
        for (K key:map.keySet()) {
            put(key, map.get(key));
        }
    }

    public V get(K key) {
        return map.get(key);
    }

    public void remove(K key) {
        // check if map contains key
        if (!map.containsKey(key)) return;
        new HashMapChange<K,V>(this, key);
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public void clear() {
        // Two-step process to avoid concurrent modification exception
        List<K> keys = new ArrayList<K>(map.keySet());
        for (K key : keys) {
            remove (key);
        }
    }

    /**
     * (re)intializes the state map from another map
     */
    public void initFromMap(Map<K,V> initMap) {
        // all from initMap get added
        putAll(initMap);
        // remove those only in current map
        for (K key:Sets.difference(map.keySet(), initMap.keySet())) {
            remove(key);
        }
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

    public String getData() {

        if (map == null) return "";

        StringBuilder buf = new StringBuilder("<html>");
        for (K name : map.keySet()) {
            if (buf.length() > 6) buf.append("<br>");
            buf.append(name.toString());
            Object value = map.get(name);
            if (value != null && rails.util.Util.hasValue(value.toString())) buf.append("=").append(value.toString());
        }
        if (buf.length() > 6) {
            buf.append("</html>");
        }
        return buf.toString();

    }
    
    @Override
    public String toString() {
        return map.toString();
    }
    
    void change(K key, V value, boolean remove) {
        if (remove) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

}
