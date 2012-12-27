package rails.game.state;

/**
 * Change associated with HashMapState
 */
class MapChange<K,V> extends Change {

    private final MapState<K,V> state;
    private final K key;
    private final V newValue;
    private final boolean remove;
    private final V oldValue;
    private final boolean existed;

    /**
     * Put element into map
     */
    MapChange(MapState<K,V> state, K key, V value) {
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
    MapChange(MapState<K,V> state, K key) {
        this.state = state;
        this.key = key;
        newValue = null;
        remove = true;
        oldValue = state.get(key);
        existed = true;
        super.init(state);
    }

    @Override void execute() {
        state.change(key, newValue, remove);
    }

    @Override void undo() {
        state.change(key, oldValue, !existed);
    }

    @Override MapState<K,V> getState() {
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
