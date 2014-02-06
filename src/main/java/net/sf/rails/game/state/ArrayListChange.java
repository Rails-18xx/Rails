package net.sf.rails.game.state;

/**
 * Change associated with ArrayListState
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
    
    @Override void execute() {
        state.change(object, index, addToList);
    }

    @Override void undo() {
        state.change(object, index, !addToList);
    }

    @Override ArrayListState<E> getState() {
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
