package rails.game.state;

/**
 * Change associated with HashMapState
 * @author evos, freystef
 */
final class HashMapChange<K,V> implements Change {

    final private HashMapState<K,V> state;
    final private K key;
    final private V newValue;
    final private boolean remove;
    final private V oldValue;
    final private boolean existed;

    /**
     * Put element into map
     */
    HashMapChange(HashMapState<K,V> state, K key, V value) {
        this.state = state;
        this.key = key;
        newValue = value;
        remove = false;
        oldValue = state.get(key);
        existed = state.containsKey(key);

        ChangeStack.add(this);
    }

    /**
     * Remove element from map
     */
    HashMapChange(HashMapState<K,V> state, K key) {
        this.state = state;
        this.key = key;
        newValue = null;
        remove = true;
        oldValue = state.get(key);
        existed = true;

        ChangeStack.add(this);
    }

    public void execute() {
        state.change(key, newValue, remove);
    }

    public void undo() {
        state.change(key, oldValue, !existed);
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "HashMapChange for " + state.getId() + ": key =  " + key + " newValue =  " + newValue +  " oldValue = " + oldValue + " remove " + remove + " existed =  " + existed;
    }

}
