package rails.game.state;

import java.util.Set;

import rails.game.model.Model;
import rails.game.model.Observer;

/**
 * An interface defining an observable object.
 * 
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 *
 * @author freystef
 *
 */
public interface Observable<E> {

    public E getData(); 

    public void addObserver(Observer<E> o);

    public boolean removeObserver(Observer<E> o);

    public Set<Observer<E>> getObservers();
    
    public void addModel(Model<?> m);
    
    public boolean removeModel(Model<?> m);
    
    public Set<Model<?>> getModels();
    
    public void updateModels();
    
}
