package rails.game.state;

import rails.game.model.Observer;

/**
 * An interface defining an observable object.
 * 
 * A very simple approach as the Observable only has to call 
 * the update() method of the observer
 *
 * @author freystef
 *
 */
public interface Observable {

    public void addObserver(Observer observer);

    public void removeObserver(Observer observer);
    
}
