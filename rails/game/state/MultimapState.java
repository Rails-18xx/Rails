package rails.game.state;

interface MultimapState<K,V> extends State {

    public boolean put(K key, V value);
    
    public boolean remove(K key, V value);
    
    public void change(K key, V value, boolean addToMap);
    
}
