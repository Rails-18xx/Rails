/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/StateChange.java,v 1.9 2008/06/04 19:00:33 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.model.ModelObject;
import rails.game.state.StateI;

/**
 * @author Erik Vos
 */
public class StateChange extends Move {

    protected StateI object;
    protected Object oldValue, newValue;
    protected ModelObject relatedModelObject;

    public StateChange(StateI object, Object newValue) {

        this(object, newValue, null);
    }

    public StateChange(StateI object, Object newValue,
            ModelObject relatedModelObject) {
        this.object = object;
        this.oldValue = object.get();
        this.newValue = newValue;
        this.relatedModelObject = relatedModelObject;

        MoveSet.add(this);
    }

    public boolean execute() {
        object.setState(newValue);
        if (relatedModelObject != null) relatedModelObject.update();
        return true;
    }

    public boolean undo() {
        object.setState(oldValue);
        if (relatedModelObject != null) relatedModelObject.update();
        return true;
    }

    public String toString() {
        return "StateChange: " + object.toString() + " from " + oldValue
               + " to " + newValue;
    }

}
