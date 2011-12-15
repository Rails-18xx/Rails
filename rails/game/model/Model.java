package rails.game.model;

import rails.game.state.Observable;

/**
 * An interface for an observable with 
 * specific information for the observers
 * This is defined by type <E>
 *  
 * @author freystef
 *
 */
public interface Model<E> extends Observable {
    
    public E getData();

}
