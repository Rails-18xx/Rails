package net.sf.rails.game.state;

final class ArrayListMultimapChange<K,V> extends Change {
    private final ArrayListMultimapState<K,V> state;
    private final K key;
    private final V value;
    private final int index;
    private final boolean addToList;

    ArrayListMultimapChange(ArrayListMultimapState<K,V> state, K key, V value, int index) {
        this.state = state;
        this.key = key;
        this.value = value;
        this.index = index;
        this.addToList = true;
        super.init(state);
    }

    ArrayListMultimapChange(ArrayListMultimapState<K,V> state, K key, int index) {
        this.state = state;
        this.key = key;
        this.value = state.get(key).get(index);
        this.index = index;
        this.addToList = false;
        super.init(state);
    }
    
    @Override void execute() {
        state.change(key, value, index, addToList);
    }

    @Override void undo() {
        state.change(key, value, index, !addToList);
    }

    @Override ArrayListMultimapState<K,V> getState() {
        return state;
    }

    @Override
    public String toString() {
        if (addToList) {
            return "Change for " + state + ": Add key = " + key + " with value " + value + " at index " + index;
        } else {
            return "Change for " + state + ": Remove object with key = " + key + " at index " + index ;
        }
    }
}
