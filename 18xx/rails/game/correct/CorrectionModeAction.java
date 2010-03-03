package rails.game.correct;

import rails.game.action.PossibleAction;
import rails.util.*;

import java.io.IOException;
import java.io.ObjectInputStream;


/**
 * Action class to request specific correction actions
 * @author freystef
 *
 */

public class CorrectionModeAction extends PossibleAction {
    
    public static final long serialVersionUID = 1L;

    // pre-conditions: type and state
    transient protected CorrectionType correction;
    protected String correctionName;
    
    protected boolean active;
    
    // post-conditions: none (except isActed!)
    
    /** 
     * Initializes with all possible correction types
     */
    public CorrectionModeAction(CorrectionType correction, boolean active) {
        this.correction = correction;
        correctionName = correction.name();
        this.active = active;
    }

    public CorrectionType getCorrection() {
        return correction;
    }

    public String getCorrectionName() {
        return correctionName;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public String getInfo(){
        return (LocalText.getText(correctionName));
    }
    
    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof CorrectionModeAction)) return false;
        CorrectionModeAction a = (CorrectionModeAction) action;
        return (a.correction == this.correction && a.active == this.active);
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("CorrectionModeAction");
        if (!acted) {
            b.append(" (not acted)");
            if (correction != null)
                b.append(", correction="+correction);
                b.append(", current state="+active);
        } else {
            b.append(" (acted)");
            if (correction != null)
                b.append(", correction="+correction);
                b.append(", previous state="+active);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(correctionName))
                correction = CorrectionType.valueOf(correctionName);
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
