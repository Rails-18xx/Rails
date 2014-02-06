package net.sf.rails.game.state;

/**
 * Change associated with BooleanState
 */

final class BooleanChange extends Change {

    private final BooleanState state;
    private final boolean newValue;
    private final boolean oldValue;
    
    BooleanChange(BooleanState state,  boolean newValue) {
        this.state = state;
        this.newValue = newValue;
        this.oldValue = state.value();
        super.init(state);
    }

    @Override void execute() {
        state.change(newValue);
    }

    @Override void undo() {
        state.change(oldValue);
    }

    @Override BooleanState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Change for " + state + ": From " + oldValue + " to " + newValue; 
    }
        
}
