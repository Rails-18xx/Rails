/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MoveSet.java,v 1.1 2007/01/23 21:50:50 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.ReportBuffer;
import rails.util.LocalText;


/**
 * @author Erik Vos
 */
public class MoveSet {

    private List moves = new ArrayList();
    
    private static MoveSet currentAction = null;
    private static List actionStack = new ArrayList();
    private static int lastIndex = -1;
    
	protected static Logger log = Logger.getLogger(MoveSet.class.getPackage().getName());

    private MoveSet () {}
    
    public static boolean start () {
        log.debug (">>> Start MoveSet");
        if (currentAction == null) {
            currentAction = new MoveSet();
            return true;
        } else {
            log.warn ("MoveSet is already open");
            return false;
        }
    }
    
    public static boolean finish () {
        log.debug ("<<< Finish MoveSet");
        if (currentAction == null) {
            log.warn  ("No action open for finish");
            return false;
        } else if (currentAction.isEmpty()) {
            log.warn ("Action to finish is empty and will be discarded");
            currentAction = null;
            return true;
        } else {
             actionStack.add (currentAction);
             lastIndex++;
             currentAction = null;
             return true;
       }
    }
    
    public static boolean cancel () {
        if (currentAction != null) {
            currentAction.unexecute();
            currentAction = null;
            return true;
        } else {
            log.warn ("No action open for cancel");
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
            log.warn ("No MoveSet open for "+move);
            //new Exception ("No MoveSet open for add: "+move).printStackTrace();
            
            return false;
        }
    }
    
    public static boolean undo () {
        if (currentAction == null && lastIndex >= 0 && lastIndex < actionStack.size()) {
            ReportBuffer.add(LocalText.getText("UNDO"));
            ((MoveSet) actionStack.get(lastIndex--)).unexecute();
            return true;
        } else {
            log.error ("Invalid undo: index="+lastIndex+" size="+actionStack.size());
            return false;
        }
    }
    
    public static boolean redo () {
        if (currentAction == null && lastIndex < actionStack.size()-1) {
            ReportBuffer.add(LocalText.getText("REDO"));
            ((MoveSet) actionStack.get(++lastIndex)).execute();
            return true;
        } else {
            log.error ("Invalid redo: index="+lastIndex+" size="+actionStack.size());
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
    
    private boolean isEmpty () {
        return moves.isEmpty();
    }
}
