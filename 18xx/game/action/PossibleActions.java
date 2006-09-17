/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/PossibleActions.java,v 1.1 2006/09/17 20:42:50 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package game.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class manages the actions that the current user can execute at any point in time.
 * Each possible action is represented by an instance of a subclass of PossibleAction.
 * The complete set is stored in a Map per action (subclass) type.
 * <p>This class is implemented as a singleton to prevent multiple instances lingering 
 * around, as there can only be one set of possible actions at any point in time. 
 * @author Erik Vos
 */
public class PossibleActions {
    
    private static PossibleActions instance = new PossibleActions();
    
    private Map possibleActions;

    /**
     * This class can only be instantiated locally.
     */
    private PossibleActions() {
        possibleActions = new HashMap();
    }
    
    public static PossibleActions getInstance() {
        return instance;
    }
    
    public void clear() {
        possibleActions.clear();
    }

    public void add (PossibleAction action) {
        Class clazz = action.getClass();
        if (!possibleActions.containsKey(clazz)) {
            possibleActions.put(clazz, new ArrayList());
        }
        ((List)possibleActions.get(clazz)).add (action);
    }
    
    public void addAll (List actions) {
        Object object;
        PossibleAction action;
        for (Iterator it = actions.iterator(); it.hasNext(); ) {
            if ((object = it.next()) instanceof PossibleAction) {
                add ((PossibleAction)object);
            } else {
                // Error!
            }
       }
    }
    
    public boolean contains (Class clazz) {
        return possibleActions.containsKey(clazz)
        	&& !((List)possibleActions.get(clazz)).isEmpty();
    }
    
    public List get (Class clazz) {
        return (List)possibleActions.get(clazz);
    }

}
