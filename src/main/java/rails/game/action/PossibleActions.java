package rails.game.action;

import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * This class manages the actions that the current user can execute at any point
 * in time. Each possible action is represented by an instance of a subclass of
 * PossibleAction. The complete set is stored in an ArrayList.
 * 
 * TODO: Should this be changed to a set?
 */
public class PossibleActions {

    private final List<PossibleAction> actions = Lists.newArrayList();

    private PossibleActions() { }

    public static PossibleActions create() {
        return new PossibleActions();
    }

    public void clear() {
        actions.clear();
    }

    public void add(PossibleAction action) {
        actions.add(action);
    }

    public void remove(PossibleAction action) {
        actions.remove(action);
    }

    public void addAll(List<? extends PossibleAction> actions) {
        this.actions.addAll(actions);
    }

    public boolean contains(Class<? extends PossibleAction> clazz) {
        for (PossibleAction action : actions) {
            if (clazz.isAssignableFrom(action.getClass())) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends PossibleAction> ImmutableList<T> getType(Class<T> clazz) {
        ImmutableList.Builder<T> result = ImmutableList.builder();
        for (PossibleAction action : actions) {
            if (clazz.isAssignableFrom(action.getClass())) {
                result.add((T) action);
            }
        }
        return result.build();
    }

    public ImmutableList<PossibleAction> getList() {
        return ImmutableList.copyOf(actions);
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public boolean containsOnlyPass() {
        if (actions.size() != 1) return false;
        PossibleAction action = actions.get(0);
        if (action instanceof NullAction && ((NullAction)action).getMode() == NullAction.Mode.PASS) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean containsCorrections() {
        for (PossibleAction action:actions) {
            if (action.isCorrection()) return true;
        }
        return false;
    }

    /** Check if a given action exists in the current list of possible actions */
    public boolean validate(PossibleAction checkedAction) {

        // Some actions are always allowed
        if (checkedAction instanceof GameAction
                && EnumSet.of(GameAction.Mode.SAVE, GameAction.Mode.RELOAD, GameAction.Mode.EXPORT).contains(
                        ((GameAction)checkedAction).getMode() )) {
            return true;
        }

        // Check if action accurs in the list of possible actions
        for (PossibleAction action : actions) {
            if (action.equalsAsOption(checkedAction)) {
                return true;
            }
        }
        return false;
    }

}
