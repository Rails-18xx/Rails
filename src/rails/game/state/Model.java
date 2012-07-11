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

    private boolean current = false;
    private String cache = null;
    
    protected Model(Item parent, String id) {
        super(parent, id);
    }

    /**
     * Calling of update informs the model that some prerequisites has been updated
     */
    public void update() {
        current = false;
    }
    
    /**
     * {@inheritDoc}
     * Remark: A Model has to either override this or cachedText(), the latter automatically caches results
     */
    @Override
    public String observerText() {
        if (!current){
            current = true;
            cache = this.cachedText();
        }
        return cache;
    }
    
    /**
     *  @return Default Model text used for Observer updates (gets cached automatically)
     */
    public String cachedText() {
        return null;
    }
    
}
