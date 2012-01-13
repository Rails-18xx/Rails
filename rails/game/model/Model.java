package rails.game.model;

import rails.game.state.Observable;

/**
 * Model is an abstract generic class
 * that defines the a middle layer between State(s) and
 * the Observer(s) in the user interface.
 * 
 * Models themselves can be layered upon each other.
 * 
 * 
 * It replaces the ModelObject class in Rails 1.0 
 * 
 * @author freystef
 */
public abstract class Model extends Observable {

    private boolean updated = false;
    private String cache = null;
    
    public Model(String id) {
        super(id);
    }
    /**
     * This method has to be used to calculate the model String
     * @return the current text of the model
     */
    protected abstract String getText();

    /**
     * Indicates that the model is updated
     * As soon as toString() it is updated via getText()
     */
    public void update() {
        updated = false;
    }
    
    @Override
    public String toString() {
        if (!updated){
            updated = true;
            cache = getText();
        }
        return cache;
    }
    
}
