package rails.game.state;

/**
 * Change associated with HashMapState
 */
final class HashMapChange<K,V> extends Change {

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
        super.init(state);
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
        super.init(state);
    }

    @Override
    public void execute() {
        state.change(key, newValue, remove);
    }

    @Override
    public void undo() {
        state.change(key, oldValue, !existed);
    }

    @Override
    public HashMapState<K,V> getState() {
        return state;
    }

    @Override
    public String toString() {
        if (!remove) {
            if (existed) {
                return "Change for " + state + ": For key=" + key + " replace value " + oldValue + " by " + newValue;
            } else {
                return "Change for " + state + ": Add key=" + key + " with value " + newValue;
            }
        } else {
            return "Change for " + state + ": Remove key=" + key + " with value " + newValue;
        }
    }

}
