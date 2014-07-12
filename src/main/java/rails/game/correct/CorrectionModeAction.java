package rails.game.correct;


import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import rails.game.action.PossibleAction;
import net.sf.rails.common.LocalText;
import net.sf.rails.util.*;


/**
 * Action class to request specific correction actions
 *
 * Rails 2.0: updated equals and toString methods
 */

public class CorrectionModeAction extends CorrectionAction {
    
    public static final long serialVersionUID = 1L;

    // pre-conditions:  state
    protected boolean active;
    
    // post-conditions: none (except isActed!)
    
    /** 
     * Initializes with all possible correction types
     */
    public CorrectionModeAction(CorrectionType correction, boolean active) {
        this.correctionType = correction;
        correctionName = correction.name();
        this.active = active;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public String getInfo(){
        return (LocalText.getText(correctionName));
    }
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // FIXME: Always allow the actions of the according type as Option
        if (asOption && pa instanceof CorrectionAction && ((CorrectionAction)pa).getCorrectionType() ==
                this.correctionType) {
            return true;
        }
        
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        CorrectionModeAction action = (CorrectionModeAction)pa;
        
        return Objects.equal(this.active, action.active);
        // no action attributes to be checked
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("active", active)
                .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(correctionName))
                correctionType = CorrectionType.valueOf(correctionName);
    }

    
//   a version with enumsets:
//    // pre-conditions
//    transient protected EnumSet<CorrectionType> possibleCorrections;
//    
//    // post-conditions
//    transient protected CorrectionType selectedCorrection;
//    
//    /** 
//     * Initializes with all possible correction types
//     */
//    public RequestCorrectionAction() {
//        possibleCorrections = EnumSet.allOf(CorrectionType.class);
//    }
//
//    /** 
//     * Initializes with a specific set of correction types
//     */
//    public RequestCorrectionAction(EnumSet<CorrectionType> possibleCorrections) {
//        this.possibleCorrections = possibleCorrections;
//    }
//
//    public EnumSet<CorrectionType> getPossibleCorrections() {
//        return possibleCorrections;
//    }
//    
//    public CorrectionType getSelectedCorrection() {
//        return selectedCorrection;
//    }
//    
//    public void setSelectedCorrection(CorrectionType selectedCorrection) {
//        this.selectedCorrection = selectedCorrection;
//    }
    

}
