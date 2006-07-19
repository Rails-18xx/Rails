/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/StateChange.java,v 1.1 2006/07/19 22:08:50 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package game.action;

/**
 * @author Erik Vos
 */
public class StateChange extends Move {
    
    protected Object oldValue, newValue, object;
    
    public StateChange (Object object, Object newValue) {
        this.oldValue = object; // DOES NOT WORK!!!
        this.newValue = newValue;
        this.object = object;
    }
    
    public boolean execute() {
       object = newValue; 
       return true;
    }

    public boolean undo() {
        object = oldValue;
        return true;
    }

}
