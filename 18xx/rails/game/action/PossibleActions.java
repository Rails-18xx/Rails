/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleActions.java,v 1.4 2007/06/01 20:24:37 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.util.ArrayList;
import java.util.HashMap;
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
    
    private Map<Class, List<PossibleAction>> possibleActionMap;
    private List<PossibleAction> possibleActionList;

    /**
     * This class can only be instantiated locally.
     */
    private PossibleActions() {
        possibleActionMap = new HashMap<Class, List<PossibleAction>>();
        possibleActionList = new ArrayList<PossibleAction>();
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
            possibleActionMap.put(clazz, new ArrayList<PossibleAction>());
        }
        ((List<PossibleAction>)possibleActionMap.get(clazz)).add (action);
        possibleActionList.add(action);
    }
    
    public void addAll (List<? extends PossibleAction> actions) {
        //Object object;
        //PossibleAction action;
        for (PossibleAction action : actions) {
            //if ((object = it.next()) instanceof PossibleAction) {
                add (action);
            //} else {
                // Error!
            //}
       }
       // possibleActionList.addAll(actions);
    }
    
    public boolean contains (Class clazz) {
        return possibleActionMap.containsKey(clazz)
        	&& !((List)possibleActionMap.get(clazz)).isEmpty();
    }
    
    // The return type cannot be generified because of problems in ORPanel
    public List getType (Class clazz) {
        return possibleActionMap.get(clazz);
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
