package rails.game.correct;

import rails.game.action.PossibleAction;;
/**
 * Base class for all actions that correct the state of the game. 
 * 
 * @author freystef
 *
 */

public abstract class CorrectionAction extends PossibleAction {
    

    transient protected CorrectionType correctionType;
    protected String correctionName;
    
    public static final long serialVersionUID = 3L;

    public CorrectionType getCorrectionType() {
        return correctionType;
    }
    
    public String getCorrectionName() {
        return correctionName;
    }
    
    public void setCorrectionType(CorrectionType correctionType) {
        this.correctionType = correctionType;
        this.correctionName = correctionType.name();
    }
}
