package net.sf.rails.game.state;

final class HashMultimapChange<K,V> extends Change {
    final private HashMultimapState<K,V> state;
    final private K key;
    final private V value;
    final private boolean addToMap;

    HashMultimapChange(HashMultimapState<K,V> state, K key, V value, boolean addToMap) {
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

    @Override HashMultimapState<K,V> getState() {
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
