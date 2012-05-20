package rails.game.state;

/**
 * Change associated with IntegerState
 * @author freystef
 */

final class IntegerChange implements Change {

    private final IntegerState state;
    private final int newValue;
    private final int oldValue;
    
    IntegerChange(IntegerState state, int newValue) {
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.value();
        ChangeStack.add(this);
    }
    
    public void execute() {
        state.change(newValue);
    }

    public void undo() {
        state.change(oldValue);
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Change IntegerState " + state.getId() + " from " + oldValue + " to " + newValue; 
    }

}
