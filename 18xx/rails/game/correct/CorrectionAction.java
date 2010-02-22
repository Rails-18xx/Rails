package rails.game.correct;

import rails.game.action.PossibleAction;;
/**
 * Base class for all actions that correct the state of the game
 * that violate the ruleset.
 * 
 * @author freystef
 *
 */

public abstract class CorrectionAction extends PossibleAction {
    
    /** shows in correction menu */
    protected boolean inCorrectionMenu;

    
    public boolean isInCorrectionMenu(){
            return inCorrectionMenu;
    }
    
    public void setCorrectionMenu(boolean menu){
        inCorrectionMenu = menu;
    }
    
}
