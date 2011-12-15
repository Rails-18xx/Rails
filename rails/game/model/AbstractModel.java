package rails.game.model;

import rails.game.state.AbstractItem;
import rails.game.state.StateManager;

/**
 * A generic superclass for all Model values that need be displayed in some form
 * in the View (UI). 
 * 
 * It combines Item (via AbstractItem), Model<E> and Observer interfaces.
 * 
 * It replaces the ModelObject class in Rails 1.0 
 * 
 * @author freystef
 */
public abstract class AbstractModel<E> extends AbstractItem implements Model<E>, Observer {

    // cached version of E
    private E cached = null;
    private boolean calculated = false;
    
    public AbstractModel(String id) {
        super(id);
    }
    
    public E getData() {
        if (!calculated) {
            cached = getLazyData();
            calculated = true;
        }
        return cached;
    }
    
    protected E getLazyData() {
        throw new AssertionError("You have to overwrite either getData() or getLazyData() for classes that extend AbstractModel.");
    }

    
    public final void update() {
        calculated = false;
    }

    // observable interface 
    public final void addObserver(Observer observer) {
        StateManager.getInstance().registerObserver(observer, this);
    }
    
    public final void removeObserver(Observer observer) {
        StateManager.getInstance().deRegisterObserver(observer);
    }
    
}
