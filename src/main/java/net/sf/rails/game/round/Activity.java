package net.sf.rails.game.round;

import rails.game.action.PossibleAction;

public interface Activity {
   
    /**
     * create actions and add them to AvailableActions
     */
    public void createActions(AvailableActions actions);
    
    /**
     * @return checks if the conditions of the actions are fullfilled
     */
    public boolean isActionExecutable(PossibleAction action);
    
    /**
     * executes the action
     */
    public void executeAction(PossibleAction action);
    
}
