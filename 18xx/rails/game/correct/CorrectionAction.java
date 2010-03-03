package rails.game.correct;

import rails.game.action.PossibleAction;;
/**
 * Base class for all actions that correct the state of the game. 
 * 
 * @author freystef
 *
 */

public abstract class CorrectionAction extends PossibleAction {
    
    private CorrectionType correctionType;
    
    public CorrectionType getCorrectionType() {
        return correctionType;
    }
    public void setCorrectionType(CorrectionType correctionType) {
        this.correctionType = correctionType;
    }
}
