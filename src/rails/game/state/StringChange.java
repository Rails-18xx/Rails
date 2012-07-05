package rails.game.state;

/**
 * Change associated with StringState
 * @author freystef
 */

final class StringChange extends Change {

    private final StringState state;
    private final String newValue;
    private final String oldValue;
    
    StringChange(StringState state, String newValue) {
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.value();
        super.init(state);
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
    public StringState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Change StringState " + state.getId() + " from " + oldValue + " to " + newValue; 
    }
        
}
