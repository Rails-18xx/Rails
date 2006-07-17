/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/Action.java,v 1.1 2006/07/17 22:00:23 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Erik Vos
 */
public class Action {

    private List moves = new ArrayList();
    
    private static Action currentAction = null;
    private static Action lastAction = null;
    private static List actionStack = new ArrayList();
    
    private Action () {}
    
    public static boolean start () {
        if (currentAction == null) {
            currentAction = new Action();
            return true;
        } else {
            System.out.println ("Action is already open");
            return false;
        }
    }
    
    public static boolean finish () {
        if (currentAction != null) {
            actionStack.add (currentAction);
            currentAction.execute();
            lastAction = currentAction;
            currentAction = null;
            return true;
        } else {
            System.out.println ("Action is not open");
            return false;
       }
    }
    
    public static boolean cancel () {
        if (currentAction != null) {
            currentAction = null;
            return true;
        } else {
            System.out.println ("Action is not open");
            return false;
         }
    }
    
    public static boolean add (Move move) {
        if (currentAction != null) {
            currentAction.moves.add (move);
        	return true;
        } else {
            System.out.println ("Action is not open");
            return false;
        }
    }
    
    public static boolean undoLast () {
        if (lastAction != null && currentAction == null) {
            lastAction.undo();
            actionStack.remove(lastAction);
            lastAction = (Action) actionStack.get(actionStack.size()-1);
            return true;
        } else {
            System.out.println ("Invalid undo");
            return false;
        }
    }
    
    public void execute () {
        
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).execute();
        }
    }
    
    public void undo () {
        // TODO: Must actually do this in reverse order
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).undo();
        }
    }
}
