package net.sf.rails.game.round;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;

public interface Activity {

    /**
     * create actions and add them to AvailableActions
     */
    public void createActions(Actor actor, PossibleActions actions);

    /**
     * @return checks if the conditions of the actions are fullfilled
     */
    public boolean isActionExecutable(PossibleAction action);

    /**
     * executes the action
     */
    public void executeAction(PossibleAction action);

    /**
     * reports action execution
     */
    public void reportExecution(PossibleAction action);

}