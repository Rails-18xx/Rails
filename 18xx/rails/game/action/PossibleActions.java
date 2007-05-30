/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleActions.java,v 1.2 2007/05/30 20:16:48 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

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
    
    private Map possibleActionMap;
    private List possibleActionList;

    /**
     * This class can only be instantiated locally.
     */
    private PossibleActions() {
        possibleActionMap = new HashMap();
        possibleActionList = new ArrayList();
    }
    
    public static PossibleActions getInstance() {
        return instance;
    }
    
    public void clear() {
        possibleActionMap.clear();
        possibleActionList.clear();
    }

    public void add (PossibleAction action) {
        Class clazz = action.getClass();
        if (!possibleActionMap.containsKey(clazz)) {
            possibleActionMap.put(clazz, new ArrayList());
        }
        ((List)possibleActionMap.get(clazz)).add (action);
        possibleActionList.add(action);
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
        possibleActionList.add(actions);
    }
    
    public boolean contains (Class clazz) {
        return possibleActionMap.containsKey(clazz)
        	&& !((List)possibleActionMap.get(clazz)).isEmpty();
    }
    
    public List getType (Class clazz) {
        return (List)possibleActionMap.get(clazz);
    }
    
    public Map getMap () {
        return possibleActionMap;
    }
    
    public List getList() {
    	return possibleActionList;
    }
    
    public boolean isEmpty() {
    	return possibleActionList.isEmpty();
    }

}
