package rails.game.state;

/**
 * Abstract class to define methods for the MultimapState classes
 */
abstract class MultimapState<K,V> extends State  {
    
    public abstract boolean put(K key, V value);
    
    public abstract boolean remove(K key, V value);
    
    abstract void change(K key, V value, boolean addToMap);
    
}
