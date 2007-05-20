/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/StateChange.java,v 1.3 2007/05/20 20:10:19 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.state.StateI;

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
        
        MoveSet.add (this);
    }
    
    public boolean execute() {
       object.setState(newValue);
       log.debug("-Done: "+toString());
       return true;
    }

    public boolean undo() {
        object.setState(oldValue);
        log.debug("-Undone: "+toString());
        return true;
    }
    
    public String toString() {
        return "StateChange: " + object.toString() 
        	+ " from " + oldValue + " to " + newValue;
    }

}
