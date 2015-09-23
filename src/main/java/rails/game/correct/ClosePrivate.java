package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import rails.game.action.PossibleAction;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Action that allows manual closure of a private company
 *
 * Rails 2.0: Updated equals and toString methods
*/
public class ClosePrivate extends PossibleAction {

    private static final long serialVersionUID = 2L;

    /* Preconditions */
    
    /** private company to close */
    transient private PrivateCompany privateCompany;
    
    /** converted to name */
    private String privateCompanyName; 

    /* Postconditions: None */
    
    public ClosePrivate(PrivateCompany priv) {
        super(null); // not defined by an activity yet
        privateCompany = priv;
        privateCompanyName = priv.getId();
    }
    
    public PrivateCompany getPrivateCompany() {
        return privateCompany;
    }
    public String getPrivateCompanyName () {
        return privateCompanyName;
    }
    
    public String getInfo(){
        return ("Close Private " + privateCompanyName);
    }
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        ClosePrivate action = (ClosePrivate)pa; 
        return Objects.equal(this.privateCompany, action.privateCompany);
        // no asAction attributes to be checked
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("privateCompany", privateCompany)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(privateCompanyName))
                privateCompany = getCompanyManager().getPrivateCompany(privateCompanyName);
    }
}
