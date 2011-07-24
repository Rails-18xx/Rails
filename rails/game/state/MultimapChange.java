package rails.game.state;

final class MultimapChange<K,V> implements Change {

    final private MultimapState<K,V> state;
    final private K key;
    final private V value;
    final private boolean addToMap;

    /**
     * Put element into map
     */
    MultimapChange(MultimapState<K,V> state, K key, V value, boolean addToMap) {
        this.state = state;
        this.key = key;
        this.value = value;
        this.addToMap = addToMap;

        ChangeStack.add(this);
    }
    
    public void execute() {
        state.change(key, value, addToMap);
    }

    public void undo() {
        state.change(key, value, !addToMap);
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "MultimapChange for " + state.getId() + ": key =  " + key + " value =  " + value + " addToMap" + addToMap;
    }

}
