package rails.game.state;

/**
 * Change associated with StringState
 * @author freystef
 */

final class StringChange implements Change {

    private final StringState state;
    private final String newValue;
    private final String oldValue;
    
    StringChange(StringState state, String newValue) {
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.stringValue();
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
        return "Change StringState " + state.getId() + " from " + oldValue + " to " + newValue; 
    }
        
}
