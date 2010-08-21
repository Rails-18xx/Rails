/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/GenericState.java,v 1.1 2010/02/26 08:04:17 stefanfrey Exp $*/
package rails.game.state;

import rails.game.model.ModelObject;
import rails.game.move.StateChange;

public final class GenericState<E> extends ModelObject implements StateI {

    private String stateName;
    private E stateObject;

    public GenericState(String name, E object) {
        stateName = name;
        if (object == null) {
            new Exception("NULL object not allowed in creating State wrapper").printStackTrace();
        } else {
            stateObject = object;
        }
    }

    public void set(E object, boolean forced) {
        if (object == null) {
            if (stateObject != null) new StateChange(this, object);
        } else if (!object.equals(stateObject) || forced)
                new StateChange(this, object);
    }

    public void set(E object) {
        set(object, false);
    }

    public void setForced(E object) {
        set(object, true);
    }

    public E get() {
        return stateObject;
    }

    /** Must only be called by the Move execute() and undo() methods */
    public void setState(Object object) {
       @SuppressWarnings("unchecked") E objE = (E)object;
        stateObject = objE;
        update();
    }

    public String getName() {
        return stateName;
    }

    @Override
    public String getText() {
        if (stateObject != null) {
            return stateObject.toString();
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return stateName;
    }
}
