package rails.game.state;

import java.util.HashSet;
import java.util.Set;

import rails.game.model.Model;
import rails.game.model.Observer;

/**
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 *
 * @author freystef
 *
 */
public abstract class Observable extends AbstractItem {

    // stores observers and models
    private Set<Observer> observers = null; // lazy initialization
    private Set<Model> models = null; // lazy initialization
    
    public Observable(String id){
        super(id);
    }
    
    public void addObserver(Observer o) {
        observers.add(o);
    }
    
    public boolean removeObserver(Observer o) {
        return observers.remove(o);
    }
    
    public Set<Observer> getObservers() {
        return observers;
    }

    public void addModel(Model m) {
        if (models == null) {
            models = new HashSet<Model>();
        }
        models.add(m);
    }
    
    public boolean removeModel(Model m) {
        return models.remove(m);
    }
    
    public Set<Model> getModels() {
        return models;
    }
    
    public void updateModels() {
        for (Model m:models) {
            m.update();
        }
    }
    
}
