package rails.game.model;

import rails.game.state.Item;
import rails.game.state.Observable;

/**
 * An interface defining all classes that convert model
 * information into something used for the UI clients  
 * 
 * @author freystef
 *
 */
public interface Model<E> extends Item, Observer, Observable {
    
    public E getData();

}
