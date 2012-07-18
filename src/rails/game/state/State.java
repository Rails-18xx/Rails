package rails.game.state;

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
    
    protected State(Item parent, String id) {
        super(parent, id);
        
        // register if observable state
        if (id != null) {
            // check if parent is a model and add as dependent model
            if (parent instanceof Model) {
                addModel((Model)parent);
            }
            this.getStateManager().registerState(this);
        }
    }
}