/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/ModelObject.java,v 1.9 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.model;

import java.util.*;

import org.apache.log4j.Logger;

import rails.util.Util;

/**
 * A generic superclass for all Model values that need be displayed in some form
 * in the View (UI). <p>This class extends Observable to allow it to be used
 * with the Observer pattern, but this usage is optional.
 */
public abstract class ModelObject extends Observable {

    protected int option = 0;

    protected Set<ModelObject> dependents = null;

    protected static Logger log =
            Logger.getLogger(ModelObject.class.getPackage().getName());

    @Override
    public void addObserver (Observer o) {
        super.addObserver(o);
        notifyViewObjects();
    }

    /** Add a dependent model object */
    public void addDependent(ModelObject object) {
        if (dependents == null) dependents = new HashSet<ModelObject>();
        /* Safe if already contained */
        dependents.add(object);
    }

    /** Remove a dependent model object. */
    public void removeDependent(ModelObject object) {
        /* Also works if it is not actually contained */
        dependents.remove(object);
    }

    private void notifyViewObjects() {
        setChanged();
        notifyObservers(getUpdate());
        clearChanged();
    }

    /** Default update is just text */
    public Object getUpdate () {
        return getText();
    }

    /**
     * Optional method, to make a subclass-dependent selection of the way the
     * "value" will be composed. The default value is 0.
     *
     * @param option The selected
     */
    public void setOption(int option) {
        this.option = option;
    }

    public void resetOption(int option) {
        log.debug("Resetting option " + option);
        this.option = Util.setBit(option, this.option, false);
    }

    /**
     * The minimum action that causes the view objects to be updated. This
     * method can be used if the object embedded in this class is just a
     * reference to an outside object (e.g. Portfolio).
     */
    public void update() {
        /* Notify the observers about the change */
        notifyViewObjects();

        /* Also update all model objects that depend on this one */
        if (dependents != null) {
            for (ModelObject object : dependents) {
                object.update();
            }
        }
    }

    /**
     * The object that is sent to the Observer along with a notification. The
     * default result is the Observable's toString(), but it can be overridden
     * where needed.
     *
     * @return
     */
    public abstract String getText();

}
