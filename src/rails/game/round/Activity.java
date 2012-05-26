package rails.game.round;

import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.state.Context;

interface Activity {
   
    /**
     * creation based on a specific context
     * @param context
     */
    public void create(Context context);
    
    /**
     * @return true if activity is active
     */
    public boolean isActive();

    /**
     * @return available actions thus checks the preconditions and creates the allowed actions 
     */
    public List<PossibleAction> getPossibleActions();
    
    /**
     * @return checks if the conditions of the actions are fullfilled
     */
    public boolean isActionAllowed(PossibleAction action);
    
    /**
     * executes the action
     */
    public void executeAction(PossibleAction action);
    
}
    


