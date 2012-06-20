package rails.game.state;

/**
 * Change associated with IntegerState
 */
final class IntegerChange extends Change {

    private final IntegerState state;
    private final int newValue;
    private final int oldValue;
    
    IntegerChange(IntegerState state, int newValue) {
        super(state);
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.value();
    }

    @Override
    public void execute() {
        state.change(newValue);
    }

    @Override
    public void undo() {
        state.change(oldValue);
    }

    @Override
    public IntegerState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Change IntegerState " + state.getId() + " from " + oldValue + " to " + newValue; 
    }

}
