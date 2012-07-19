package rails.game.state;

final class MultimapChange<K,V> extends Change {
    final private MultimapState<K,V> state;
    final private K key;
    final private V value;
    final private boolean addToMap;

    MultimapChange(MultimapState<K,V> state, K key, V value, boolean addToMap) {
        this.state = state;
        this.key = key;
        this.value = value;
        this.addToMap = addToMap;
        super.init(state);
    }
    
    @Override void execute() {
        state.change(key, value, addToMap);
    }

    @Override void undo() {
        state.change(key, value, !addToMap);
    }

    @Override MultimapState<K,V> getState() {
        return state;
    }

    @Override
    public String toString() {
        if (addToMap) {
            return "Change for " + state + ": Add key = " + key + " with value " + value;
        } else {
            return "Change for " + state + ": Remove key = " + key + " with value " + value;
        }
    }
}
