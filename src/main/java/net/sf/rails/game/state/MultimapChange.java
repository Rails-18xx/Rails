package net.sf.rails.game.state;

public final class MultimapChange<K,V> extends Change {
    private final MultimapState<K,V> state;
    private final K key;
    private final V value;
    private final boolean addToMap;

    MultimapChange(MultimapState<K,V> state, K key, V value, boolean addToMap) {
        this.state = state;
        this.key = key;
        this.value = value;
        this.addToMap = addToMap;
        super.init(state);
    }
    
    @Override 
    void execute() {
        state.change(key, value, addToMap);
    }

    @Override 
    void undo() {
        state.change(key, value, !addToMap);
    }

    @Override
    public 
    MultimapState<K,V> getState() {
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
