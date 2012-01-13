package rails.game.state;

/**
 * Change associated with HashSetState
 * 
 * @author Brett Lentz, freystef
 */
final class HashSetChange<E> implements Change {
    final private HashSetState<E> state;
    final private E element;
    final private boolean addToSet;

    /**
     * Add/Remove element to/from the set
     */
    
    HashSetChange(HashSetState<E> state, E element, boolean addToSet){
        this.state = state;
        this.element = element;
        this.addToSet = addToSet;
        
        ChangeStack.add(this);
    }

    public void execute() {
        state.change(element, addToSet);
    }

    public void undo() {
        state.change(element, !addToSet);
    }
    
    public State getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return "HashSetChange for " + state.getId() + ": " + element.toString() + " addToSet = " + addToSet;
    }

}
