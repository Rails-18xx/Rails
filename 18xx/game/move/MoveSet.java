/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/MoveSet.java,v 1.1 2006/09/14 19:33:31 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Erik Vos
 */
public class MoveSet {

    private List moves = new ArrayList();
    
    private static MoveSet currentAction = null;
    private static List actionStack = new ArrayList();
    private static int lastIndex = -1;
    
    private MoveSet () {}
    
    public static boolean start () {
        //System.out.println(">>> Start MoveSet");
        if (currentAction == null) {
            currentAction = new MoveSet();
            return true;
        } else {
            System.out.println ("MoveSet is already open");
            return false;
        }
    }
    
    public static boolean finish () {
        //System.out.println("<<< Finish MoveSet");
        if (currentAction != null) {
            actionStack.add (currentAction);
            lastIndex++;
            currentAction = null;
            return true;
        } else {
            System.out.println ("No action open for finish");
            return false;
       }
    }
    
    public static boolean cancel () {
        if (currentAction != null) {
            currentAction.unexecute();
            currentAction = null;
            return true;
        } else {
            System.out.println ("No action open for cancel");
            return false;
         }
    }
    
    public static boolean add (Move move) {

        move.execute();
        if (currentAction != null) {
            currentAction.moves.add (0, move); // Prepare for undo in reverse order!
        	return true;
        } else {
            // Uncomment one of the next statements to detect un-undoable actions
            //System.out.println ("No MoveSet open for "+move);
            //new Exception ("No MoveSet open for add: "+move).printStackTrace();
            
            return false;
        }
    }
    
    public static boolean undo () {
        if (currentAction == null && lastIndex >= 0 && lastIndex < actionStack.size()) {
            ((MoveSet) actionStack.get(lastIndex--)).unexecute();
            return true;
        } else {
            System.out.println ("Invalid undo: index="+lastIndex+" size="+actionStack.size());
            return false;
        }
    }
    
    public static boolean redo () {
        if (currentAction == null && lastIndex < actionStack.size()-1) {
            ((MoveSet) actionStack.get(++lastIndex)).execute();
            return true;
        } else {
            System.out.println ("Invalid redo: index="+lastIndex+" size="+actionStack.size());
            return false;
        }
    }
    
    public static boolean isUndoable() {
        return lastIndex >= 0;
    }
    
    public static boolean isRedoable() {
        return lastIndex < actionStack.size()-1;
    }
    
    public static boolean isOpen() {
        return currentAction != null;
    }
    
    /**
     * Clear the whole stack.
     * To be used if a state change occurs that cannot (yet) be undone. 
     * @return
     */
    public static boolean clear () {
        if (currentAction != null) currentAction.execute();
        actionStack = new ArrayList();
        currentAction = null;
        lastIndex = -1;
        return true;
        
    }
    
    private void execute () {
        
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).execute();
        }
    }
    
    private void unexecute () {

        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).undo();
        }
    }
}
