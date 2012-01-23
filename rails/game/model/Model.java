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
     * Indicates that the model is updated, so the getText() cache
     * is flushed
     */
    public void update() {
        updated = false;
    }
    
    /**
     * For a model the text shown to observer is derived from toString()
     * The value is cached until the model is updated
     */
    @Override
    public final String getText() {
        if (!updated){
            updated = true;
            cache = toString();
        }
        return cache;
    }
    
}
