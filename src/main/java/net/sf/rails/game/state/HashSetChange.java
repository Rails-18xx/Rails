package net.sf.rails.game.state;

/**
 * Change associated with HashSetState
 */
final class HashSetChange<E> extends Change {
    private final HashSetState<E> state;
    private final E element;
    private final boolean addToSet;

    /**
     * Add/Remove element to/from the set
     */
    HashSetChange(HashSetState<E> state, E element, boolean addToSet){
        this.state = state;
        this.element = element;
        this.addToSet = addToSet;
        super.init(state);
    }

    @Override void execute() {
        state.change(element, addToSet);
    }

    @Override void undo() {
        state.change(element, !addToSet);
    }
    
    @Override HashSetState<E> getState() {
        return state;
    }
    
    @Override
    public String toString() {
        if (addToSet) {
            return "Change for " + state + ": Add " + element;
        } else {
            return "Change for " + state + ": Remove " + element;
        }
    }

}
