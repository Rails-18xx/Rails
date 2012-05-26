package rails.game.state;

/**
 * Change associated with ArrayListState
 * @author evos, freystef
 */
final class ArrayListChange<E> implements Change {
    final private ArrayListState<E> state;
    final private E object;
    final private int index;
    final private boolean addToList;

    /**
     * Add object to the end of the list
     */
    ArrayListChange(ArrayListState<E> state, E object) {
        this(state, object, state.size() + 1);
    }

    /**
     * Add object at the specified index
     */
    ArrayListChange(ArrayListState<E> state, E object, int index) {
        this.state = state;
        this.object = object;
        this.index = index;
        this.addToList = true;
        ChangeStack.add(this);
    }

    /**
     * Remove object at the specified index
     */
    ArrayListChange(ArrayListState<E> state, int index) {
        this.state = state;
        this.object = state.get(index);
        this.index = index;
        this.addToList = false;
    }

    public void execute() {
        state.change(object, index, addToList);
    }

    public void undo() {
        state.change(object, index, !addToList);
    }

    public State getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return "ArrayListChange for " + state.getId() + ": " + object.toString() + " at index " + index + " addToList = " + addToList;
    }

}
