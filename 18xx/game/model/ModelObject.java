package game.model;

import java.util.Observable;

import org.apache.log4j.Logger;

/**
 * A generic superclass for all Model values that need be displayed
 * in some form in the View (UI).
 * <p>This class extends Observable to allow it to be used with the Observer
 * pattern, but this usage is optional.
 */
public abstract class ModelObject extends Observable {
    
    protected int option = 0;
    
	protected static Logger log = Logger.getLogger(ModelObject.class.getPackage().getName());

    protected void notifyViewObjects() {
        setChanged();
        notifyObservers (getNotificationObject());
        //log.debug ("'"+getNotificationObject()+"' sent from "+getClass().getName());
        clearChanged();
    }
    
    /**
     * Optional method, to make a subclass-dependent selection
     * of the way the "value" will be composed. The default value is 0.
     * <p>The return value allows to glue the call to this method to any other one
     * that returns this type (see usage in class Field).
     * @param option Selection of the ModelObject's value.
     * @return This object.
     */
    public ModelObject option (int option) {
        this.option = option;
        return this; // To facilitate usage (see class Field)
    }
    
    /**
     * The minimum action that causes the view objects to be updated.
     * This method can be used if the object embedded in this class
     * is just a reference to an outside object (e.g. Portfolio).  
     */
    public void update () {
        notifyViewObjects();
    }
    
    /**
     * The object that is sent to the Observer along with a notification.
     * The default result is the Observable's toString(), but it can
     * be overridden where needed.  
     * @return
     */
    public Object getNotificationObject() {
        return toString();
    }

}
