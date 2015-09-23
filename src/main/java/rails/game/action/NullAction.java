package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;

/**
 * 
 * Rails 2.0: Updated equals and toString methods
 */
public class NullAction extends PossibleAction {
    
    private static final long serialVersionUID = 2L;

    public static enum Mode { DONE, PASS, SKIP, AUTOPASS, START_GAME }

    // optional label that is returned on toString instead of the standard labels defined below
    private String optionalLabel = null;

    protected transient Mode mode_enum = null;
    // Remark: it would have been better to store the enum name, however due to backward compatibility not an option
    protected int mode; 

    public NullAction(Mode mode) {
        super(null); // not defined by an activity yet
        this.mode_enum = mode;
        this.mode = mode.ordinal();
    }

    public Mode getMode() {
        return mode_enum;
    }
    
    /** returns the NullAction itself */
    public NullAction setLabel(String label) {
        this.optionalLabel = label;
        return this;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        NullAction action = (NullAction)pa; 
        return Objects.equal(this.mode, action.mode)
                && Objects.equal(this.optionalLabel, action.optionalLabel)
        ;
        // no asAction attributes to be checked
    }

   @Override
    public String toString() {
       return super.toString() + 
               RailsObjects.stringHelper(this)
                   .addToString("mode", mode_enum)
                   .addToString("optionalLabel", optionalLabel)
                   .toString()
       ;
    }

   private void readObject(ObjectInputStream in) throws IOException,
           ClassNotFoundException {

       in.defaultReadObject();
       // required since Rails 2.0
       mode_enum = Mode.values()[mode];
   }

}
