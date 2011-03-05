package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.*;
import rails.game.action.PossibleAction;
import rails.util.Util;

/**
 * Action that allows manual closure of a private company
 */

public class ClosePrivate extends PossibleAction {

    private static final long serialVersionUID = 2L;

    /* Preconditions */
    
    /** private company to close */
    private PrivateCompanyI privateCompany;
    
    /** converted to name */
    private String privateCompanyName; 

    /* Postconditions: None */
    
    public ClosePrivate(PrivateCompanyI priv) {
        privateCompany = priv;
        privateCompanyName = priv.getName();
    }
    
    public PrivateCompanyI getPrivateCompany() {
        return privateCompany;
    }
    public String getPrivateCompanyName () {
        return privateCompanyName;
    }
    
    public String getInfo(){
        return ("Close Private " + privateCompanyName);
    }
    
    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof ClosePrivate)) return false;
        ClosePrivate a = (ClosePrivate) action;
        return (a.privateCompany == this.privateCompany);
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        return action.equalsAsOption(this);
    }
    
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("ClosePrivate");
        if (!acted) {
            b.append(" (not acted)");
            if (privateCompany != null)
                b.append(", privateCompany="+privateCompany);
        } else {
            b.append(" (acted)");
            if (privateCompany != null)
                b.append(", privateCompany="+privateCompany);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(privateCompanyName))
                privateCompany = getCompanyManager().getPrivateCompany(privateCompanyName);
    }
}
