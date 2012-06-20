package rails.game.state;

/**
 * Change associated with HashSetState
 */
final class HashSetChange<E> extends Change {
    final private HashSetState<E> state;
    final private E element;
    final private boolean addToSet;

    /**
     * Add/Remove element to/from the set
     */
    HashSetChange(HashSetState<E> state, E element, boolean addToSet){
        super(state);
        this.state = state;
        this.element = element;
        this.addToSet = addToSet;
    }

    @Override
    public void execute() {
        state.change(element, addToSet);
    }

    @Override
    public void undo() {
        state.change(element, !addToSet);
    }
    
    @Override
    public HashSetState<E> getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return "HashSetChange for " + state.getId() + ": " + element.toString() + " addToSet = " + addToSet;
    }

}
