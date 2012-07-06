package rails.game.state;

import static com.google.common.base.Preconditions.*;

/**
 * State is an abstract generic class
 * that defines the base layer of objects that contain game state.
 * 
 * All State(s) are Item(s) themselves.
 * 
 * It allows to add a Formatter to change the String output dynamically.
 * 
 * 
 * States get register with the StateManager after initialization
 */
public abstract class State extends Observable {
    
    // optional formatter
    private Formatter<State> formatter = null;

    protected State(Item parent, String id) {
        super(parent, id);
        
        
        // register if observable state
        if (id != null) {
            // check if parent is a model and add as dependent model
            if (parent instanceof Model) {
                addModel((Model)parent);
            }
            // check if there is a StateManager available
            checkState(getContext().getRoot().getStateManager() != null, "Root of state has no StateManager attached");
            // if so => register state there
            getRoot().getStateManager().registerState(this);
        }
    }
    
    // Observable methods
    /**
     * For a state getText() it is identical to toString()
     * If this does not work use a formatter
     */
    @Override
    public final String getText() {
        if (formatter == null) {
            return toString();
        } else {
            return formatter.formatValue(this);
        }
    }

    /**
     * Adds a Formatter 
     * @param formatter
     */
    public void setFormatter(Formatter<State> formatter) {
        this.formatter = formatter;
    }
}