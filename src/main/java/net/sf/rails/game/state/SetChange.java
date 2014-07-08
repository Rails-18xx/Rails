package net.sf.rails.game.state;

/**
 * Change associated with HashSetState
 */
public final class SetChange<E> extends Change {
    private final SetState<E> state;
    private final E element;
    private final boolean addToSet;

    /**
     * Add/Remove element to/from the set
     */
    SetChange(SetState<E> state, E element, boolean addToSet){
        this.state = state;
        this.element = element;
        this.addToSet = addToSet;
        super.init(state);
    }

    @Override 
    void execute() {
        state.change(element, addToSet);
    }

    @Override 
    void undo() {
        state.change(element, !addToSet);
    }
    
    @Override
    public SetState<E> getState() {
        return state;
    }
    
    public boolean isAddToSet() {
        return addToSet;
    }
    
    public E getElement() {
        return element;
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
