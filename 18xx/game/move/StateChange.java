/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/StateChange.java,v 1.1 2006/09/14 19:33:31 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package game.move;

import game.state.StateI;

/**
 * @author Erik Vos
 */
public class StateChange extends Move {
    
    protected StateI object;
    protected Object oldValue, newValue;
    
    public StateChange (StateI object, Object newValue) {
        this.object = object;
        this.oldValue = object.getState();
        this.newValue = newValue;
    }
    
    public boolean execute() {
       object.setState(newValue); 
       return true;
    }

    public boolean undo() {
        object.setState(oldValue);
        return true;
    }
    
    public String toString() {
        return object.toString() + " from " + oldValue + " to " + newValue;
    }

}
