package rails.game.state;

/**
 * Change associated with BooleanState
 * @author freystef
 */

final class BooleanChange implements Change {

    private final BooleanState state;
    private final boolean newValue;
    private final boolean oldValue;
    
    BooleanChange(BooleanState state,  boolean newValue) {
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.booleanValue();
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
        return "Change BooleanState " + state.getId() + " from " + oldValue + " to " + newValue; 
    }
        
}
