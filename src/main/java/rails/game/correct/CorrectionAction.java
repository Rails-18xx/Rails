package rails.game.correct;

import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;

import rails.game.action.PossibleAction;
/**
 * Base class for all actions that correct the state of the game. 
 * 
 * Rails 2.0: updated equals and toString methods
 */
public abstract class CorrectionAction extends PossibleAction {

    transient protected CorrectionType correctionType;
    protected String correctionName;
    
    public static final long serialVersionUID = 3L;

    public CorrectionAction() {
        super(null); // not defined by an activity yet
    }

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
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        CorrectionAction action = (CorrectionAction)pa;
        return Objects.equal(this.correctionType, action.correctionType);
        // no action attributes to be checked
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("correctionType", correctionType)
                .toString()
        ;
    }
}
