/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/UseSpecialProperty.java,v 1.4 2008/06/04 19:00:29 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.special.SpecialProperty;
import rails.game.special.SpecialPropertyI;

/**
 * @author Erik Vos
 */
public class UseSpecialProperty extends PossibleORAction {

    /*--- Preconditions ---*/

    /** The special property that could be used */
    transient protected SpecialPropertyI specialProperty = null;
    private int specialPropertyId;

    /*--- Postconditions ---*/

    public UseSpecialProperty(SpecialPropertyI specialProperty) {
        super();
        this.specialProperty = specialProperty;
        if (specialProperty != null)
            this.specialPropertyId = specialProperty.getUniqueId();
    }

    public static final long serialVersionUID = 1L;

    /**
     * @return Returns the specialProperty.
     */
    public SpecialPropertyI getSpecialProperty() {
        return specialProperty;
    }

    public boolean equals(PossibleAction action) {
        if (!(action instanceof UseSpecialProperty)) return false;
        UseSpecialProperty a = (UseSpecialProperty) action;
        return a.specialProperty == specialProperty;
    }

    public String toString() {
        StringBuffer b = new StringBuffer("UseSpecialProperty: ");
        if (specialProperty != null) b.append(specialProperty);
        return b.toString();
    }

    public String toMenu() {
        return specialProperty.toMenu();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (specialPropertyId > 0) {
            specialProperty =
                    (SpecialPropertyI) SpecialProperty.getByUniqueId(specialPropertyId);
        }
    }

}
