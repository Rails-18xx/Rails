package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.util.RailsObjects;


/**
 * This class can only be used to offer a Special Property to the UI that does
 * NOT need any return parameters. Example: the M&H/NYC swap in 1830.
 * 
 * Rails 2.0: Updated equals and toString methods
 */
public class UseSpecialProperty extends PossibleORAction {

    /*--- Preconditions ---*/

    /** The special property that could be used */
    transient protected SpecialProperty specialProperty = null;
    private int specialPropertyId;

    /*--- Postconditions ---*/

    public UseSpecialProperty(SpecialProperty specialProperty) {
        super();
        this.specialProperty = specialProperty;
        if (specialProperty != null)
            this.specialPropertyId = specialProperty.getUniqueId();
    }

    public static final long serialVersionUID = 1L;

    /**
     * @return Returns the specialProperty.
     */
    public SpecialProperty getSpecialProperty() {
        return specialProperty;
    }

    public String toMenu() {
        return specialProperty.toMenu();
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        UseSpecialProperty action = (UseSpecialProperty)pa; 
        return Objects.equal(this.specialProperty, action.specialProperty);
        // no asAction attributes to be checked
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("specialProperty", specialProperty)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (specialPropertyId > 0) {
            specialProperty = SpecialProperty.getByUniqueId(specialPropertyId);
        }
    }

}
