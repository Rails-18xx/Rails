/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/ModelObject.java,v 1.2 2005/12/11 21:06:49 evos Exp $
 * 
 * Created on 08-Dec-2005
 * Change Log:
 */
package game.model;

import java.util.Observable;

/**
 * A generic superclass for all Model values that need be displayed
 * in some form in the View (UI).
 * <p>This class extends Observable to allow it to be used with the Observer
 * pattern, but this usage is optional.
 * @author Erik Vos
 */
public abstract class ModelObject extends Observable {
    
    protected int option = 0;
    
    protected void notifyViewObjects() {
        setChanged();
        notifyObservers (toString());
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
     * The minumum action that causes the view objects to be updated.
     * This method can be used if the object embedded in this class
     * is just a reference to an outside object (e.g. Portfolio).  
     */
    public void update () {
        notifyViewObjects();
    }

}
