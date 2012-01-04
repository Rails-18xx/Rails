package rails.game.model;

import java.util.HashSet;
import java.util.Set;

import rails.game.state.AbstractItem;
import rails.game.state.Observable;


/**
 * Model is an abstract generic class
 * that defines the a middle layer between State(s) and
 * the Observer(s) in the user interface.
 * 
 * Models themselves can be layered upon each other.
 * 
 * It implements the following interfaces
 * Observable<E>: Allows to update other Model(s) or other Observer(s)
 * 
 * All Model(s) are Item(s) themselves
 * 
 * It replaces the ModelObject class in Rails 1.0 
 * 
 * @author freystef
 */
public abstract class Model<E> extends AbstractItem implements Observable<E> {

    // cached version of E
    private E cached = null;
    private boolean calculated = false;
    
    // stores observers and models
    private Set<Observer<E>> observers = null; // lazy initialization
    private Set<Model<?>> models = null; // lazy initialization
    
    public Model(String id) {
        super(id);
    }

    // methods for model
    public final void update() {
        calculated = false;
    }
    
    // observable interface 
    public E getData() {
        if (!calculated) {
            cached = getLazyData();
            calculated = true;
        }
        return cached;
    }

    // allows to implement a lazy approach to the update
    protected E getLazyData() {
        throw new AssertionError("You have to overwrite either getData() or getLazyData() for classes that extend AbstractModel.");
    }
    
    public void addObserver(Observer<E> o) {
        if (observers == null) {
            observers = new HashSet<Observer<E>>();
        }
        observers.add(o);
    }

    public boolean removeObserver(Observer<E> o) {
        return observers.remove(o);
    }
    
    public Set<Observer<E>> getObservers() {
        return observers;
    }
    
    public void addModel(Model<?> m) {
        if (models == null) {
            models = new HashSet<Model<?>>();
        }
        models.add(m);
    }
    
    public boolean removeModel(Model<?> m) {
        return models.remove(m);
    }

    public Set<Model<?>> getModels() {
        return models;
    }

    public void updateModels() {
        for (Model<?> m:models) {
            m.update();
        }
    }

}
