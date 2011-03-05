/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleActions.java,v 1.17 2010/04/04 22:02:53 stefanfrey Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This class manages the actions that the current user can execute at any point
 * in time. Each possible action is represented by an instance of a subclass of
 * PossibleAction. The complete set is stored in aan ArrayList.
 * <p>This class is implemented as a singleton to prevent multiple
 * instances lingering around, as there can only be one set of possible actions
 * at any point in time.
 *
 * @author Erik Vos
 */
public class PossibleActions {

    private static PossibleActions instance = new PossibleActions();

    private List<PossibleAction> possibleActions;

    protected static Logger log =
            Logger.getLogger(PossibleActions.class.getPackage().getName());

    /**
     * This class can only be instantiated locally.
     */
    private PossibleActions() {
        possibleActions = new ArrayList<PossibleAction>();

    }

    public static PossibleActions getInstance() {
        return instance;
    }

    public void clear() {
        possibleActions.clear();
    }

    public void add(PossibleAction action) {
        possibleActions.add(action);
    }

    public void remove(PossibleAction action) {
        possibleActions.remove(action);
    }

    public void addAll(List<? extends PossibleAction> actions) {
        for (PossibleAction action : actions) {
            add(action);
        }
    }

    public boolean contains(Class<? extends PossibleAction> clazz) {
        for (PossibleAction action : possibleActions) {
            if (clazz.isAssignableFrom(action.getClass())) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends PossibleAction> List<T> getType(Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        for (PossibleAction action : possibleActions) {
            if (clazz.isAssignableFrom(action.getClass())) result.add((T) action);
        }
        return result;
    }

    public List<PossibleAction> getList() {
        return possibleActions;
    }

    public boolean isEmpty() {
        return possibleActions.isEmpty();
    }
    
    public boolean containsOnlyPass() {
        if (possibleActions.size() != 1) return false;
        PossibleAction action = possibleActions.get(0);
        if (action instanceof NullAction && ((NullAction)action).getMode() == NullAction.PASS) {
            return true;
        } else {
            return false;
        }
    }

    /** Check if a given action exists in the current list of possible actions */
    public boolean validate(PossibleAction checkedAction) {

        // Some actions are always allowed
        if (checkedAction instanceof GameAction
                && (((GameAction)checkedAction).getMode() == GameAction.SAVE
                    || ((GameAction)checkedAction).getMode() == GameAction.RELOAD
                    || ((GameAction)checkedAction).getMode() == GameAction.EXPORT)) {
            return true;
        }

        // Check if action accurs in the list of possible actions
        for (PossibleAction action : possibleActions) {
            if (action.equalsAsOption(checkedAction)) {
                return true;
            }
        }
        return false;
    }

}
