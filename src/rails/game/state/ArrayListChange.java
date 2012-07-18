package rails.game.state;

/**
 * Change associated with ArrayListState
 * @author evos, freystef
 */
final class ArrayListChange<E> extends Change {
    private final ArrayListState<E> state;
    private final E object;
    private final int index;
    private final boolean addToList;

    /**
     * Add object at the specified index
     */
    ArrayListChange(ArrayListState<E> state, E object, int index) {
        this.state = state;
        this.object = object;
        this.index = index;
        this.addToList = true;
        super.init(state);
    }

    /**
     * Remove object at the specified index
     */
    ArrayListChange(ArrayListState<E> state, int index) {
        this.state = state;
        this.object = state.get(index);
        this.index = index;
        this.addToList = false;
        super.init(state);
    }
    
    @Override
    public void execute() {
        state.change(object, index, addToList);
    }

    @Override
    public void undo() {
        state.change(object, index, !addToList);
    }

    @Override
    public ArrayListState<E> getState() {
        return state;
    }
    
    @Override
    public String toString() {
        if (addToList) {
            return "Change for " + state + ": Add " + object + " at index " + index;
        } else {
            return "Change for " + state + ": Remove " + object + " at index " + index;
        }
    }

}
