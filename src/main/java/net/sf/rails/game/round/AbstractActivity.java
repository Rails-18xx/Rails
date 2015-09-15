package net.sf.rails.game.round;

import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.state.BooleanState;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;

public abstract class AbstractActivity extends RailsAbstractItem implements Activity {
   
    private final BooleanState enabled = BooleanState.create(this, "enabled");
    
    protected AbstractActivity(RoundNG parent, String id) {
        super(parent, id);
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isEnabled() {
        return enabled.value();
    }
    
    /**
     * create actions and add them to AvailableActions
     */
    @Override
    public abstract void createActions(Actor actor, PossibleActions actions);
    
    /**
     * @return checks if the conditions of the actions are fullfilled
     */
    @Override
    public abstract boolean isActionExecutable(PossibleAction action);
    
    /**
     * executes the action
     */
    @Override
    public abstract void executeAction(PossibleAction action);
    
    /**
     * reports action execution
     */
    @Override
    public abstract void reportExecution(PossibleAction action);
    
}
