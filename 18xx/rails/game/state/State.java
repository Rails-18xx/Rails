/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/State.java,v 1.15 2010/01/31 22:22:33 macfreek Exp $*/
package rails.game.state;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;
import rails.game.move.StateChange;

public class State extends ModelObject implements StateI {

    protected String name;
    protected Object object = null;
    protected Class<?> clazz = null;

    protected static Logger log =
            Logger.getLogger(State.class.getPackage().getName());

    public State(String name, Class<?> clazz) {
        this.name = name;
        if (clazz == null) {
            new Exception("NULL class not allowed in creating State wrapper").printStackTrace();
        } else {
            this.clazz = clazz;
        }
    }

    public State(String name, Object object) {
        this.name = name;
        if (object == null) {
            new Exception("NULL object not allowed in creating State wrapper").printStackTrace();
        } else if (clazz != null && clazz.isAssignableFrom(object.getClass())) {
            new Exception("Object " + object + " must be instance of " + clazz).printStackTrace();
        } else {
            this.object = object;
            if (clazz == null) clazz = object.getClass();
        }
    }

    public void set(Object object, boolean forced) {
        if (object == null) {
            if (this.object != null) new StateChange(this, object);
        //} else if (Util.isInstanceOf(object, clazz)) {
        } else if (clazz.isAssignableFrom(object.getClass())) {
            if (!object.equals(this.object) || forced)
                new StateChange(this, object);
        } else {
            log.error("Incompatible object type " + object.getClass().getName()
                      + " passed to " + getClassName()
                      + " wrapper for object type " + clazz.getName() + " at:",
                    new Exception(""));
        }
    }

    public void set(Object object) {
        set(object, false);
    }

    public void setForced(Object object) {
        set(object, true);
    }

    public Object get() {
        return object;
    }

    /** Must only be called by the Move execute() and undo() methods */
    public void setState(Object object) {
        this.object = object;
        //log.debug (getClassName() + " "+name+" set to "+object);
        update();
    }

    public String getName() {
        return name;
    }

    @Override
    public String getText() {
        if (object != null) {
            return object.toString();
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String getClassName() {
        return getClass().getName().replaceAll(".*\\.", "");
    }

}
