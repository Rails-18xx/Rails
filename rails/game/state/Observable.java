package rails.game.state;

import java.util.HashSet;
import java.util.Set;

import rails.game.model.Model;

/**
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 *
 * @author freystef
 *
 */
public abstract class Observable extends GameItem {

    // stores observers and models
    private Set<Observer> observers = null; // lazy initialization
    private Set<Model> models = null; // lazy initialization
    
    public Observable(String id){
        super(id);
    }
    
    public void addObserver(Observer o) {
        if (observers == null) {
            observers = new HashSet<Observer>();
        }
        observers.add(o);
    }
    
    public boolean removeObserver(Observer o) {
        if (observers == null) return false;
        
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
        if (models == null) return false;
        
        return models.remove(m);
    }
    
    public Set<Model> getModels() {
        return models;
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
