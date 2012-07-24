package rails.game.state;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A stateful version of a HashMap
 */
public final class HashMapState<K,V> extends State {

    private final HashMap<K,V> map;

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
     * Add key,value pair to map
     * @param key for mapping
     * @param value associated with key 
     * @return previous value associated with specified key, or null if there was no mapping for the key (or null was the value).
     */
    public V put(K key, V value) {
        // check if the key is in the map
        if (map.containsKey(key)) {
            V oldValue = map.get(key);
            // check if element already has the specified value
            if (!oldValue.equals(value)) {
                new HashMapChange<K,V>(this, key, value);
            }
            return oldValue;
        } else {
            // if not in map, add tuple and return null
            new HashMapChange<K,V>(this, key, value);
            return null;
        }
    }
    
    /**
     * Adds all (key,value) pairs
     * @param map that gets added
     * @throws NullPointerException if map is null
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
     * @return value associated with the key, null if map does not contain key
     */
    public V get(K key) {
        return map.get(key);
    }

    /**
     * removes key from mapping
     * @param key to be removed from map
     * @return value previously associated with key, null if map did not contain key
     */
    public V remove(K key) {
        // check if map contains key
        if (!map.containsKey(key)) return null;
        V old = map.get(key);
        new HashMapChange<K,V>(this, key);
        return old;
    }

    /**
     * test if key is present in mapping
     * @param key whose presence is tested
     * @return true if key is present
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * removes all mappings from the map
     */
    public void clear() {
        for (K key : ImmutableSet.copyOf(map.keySet())) {
            remove (key);
        }
    }

    /**
     * checks if map is empty
     * @return true if map is empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * (re)initializes the state map from another map
     * @param map used for initialization
     */
    public void initFromMap(Map<K,V> initMap) {
        // all from initMap get added
        putAll(initMap);
        // remove those only in current map
        for (K key:ImmutableSet.copyOf(Sets.difference(map.keySet(), initMap.keySet()))) {
            remove(key);
        }
    }

    /**
     * creates an immutable copy of the map
     * @return immutable version of the map
     */
    public ImmutableMap<K,V> viewMap() {
        return ImmutableMap.copyOf(map);
    }

    /**
     * creates an immutable copy of the keyset
     * @return immutable keyset of the map
     */
    public ImmutableSet<K> viewKeySet() {
        return ImmutableSet.copyOf(map.keySet());
    }

    /**
     * creates an immutable copy of the values
     * @return immutable list of values
     */
    public ImmutableList<V> viewValues() {
        return ImmutableList.copyOf(map.values());
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
    public String observerText() {
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
