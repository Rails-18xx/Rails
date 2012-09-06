package rails.game.state;

/**
 * Model is an abstract generic class
 * that defines the a middle layer between State(s) and
 * the Observer(s) in the user interface.
 * 
 * Models themselves can be layered upon each other.
 * 
 * It replaces the ModelObject class in Rails 1.0 
 */

public abstract class Model extends Observable {

    protected Model(Item parent, String id) {
        super(parent, id);
    }

    /**
     * Calling of update informs the model that some state has changed
     * Overriding this does not require a call, as it usually does nothing
     */
    public void update(Change change) {
        // Standard behavior is do nothing
    }
    
}
