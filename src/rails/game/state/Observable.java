package rails.game.state;

import com.google.common.collect.ImmutableSet;


/**
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 *
 * @author freystef
 *
 */
public abstract class Observable extends AbstractItem {

    // stores observers and models
    private HashSetState<Observer> observers = null; // lazy initialization
    private HashSetState<Model> models = null; // lazy initialization
    
    protected Observable(Item parent, String id) {
        super(parent, id);
    }

    public void addObserver(Observer o) {
        if (observers == null) {
            observers = HashSetState.create(this, "observers");
        }
        observers.add(o);
    }
    
    public boolean removeObserver(Observer o) {
        if (observers == null) return false;
        
        return observers.remove(o);
    }
    
    public ImmutableSet<Observer> getObservers() {
        return observers.view();
    }

    public void addModel(Model m) {
        if (models == null) {
            models = HashSetState.create(this, "models");
        }
        
        models.add(m);
    }
    
    public boolean removeModel(Model m) {
        if (models == null) return false;
        
        return models.remove(m);
    }
    
    public ImmutableSet<Model> getModels() {
        return models.view();
    }
    
    public void updateModels() {
        if (models == null) return;
        
        for (Model m:models) {
            m.update();
        }
    }
    
    /**
     * @return text to be read by observers
     */
    public abstract String getText();
    
}
