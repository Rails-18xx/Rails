package rails.game.correct;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.*;
import rails.game.action.PossibleAction;
import rails.util.Util;

/**
 * Correction action that allows the closure of a private company.
 */

public class ClosePrivate extends PossibleAction implements CorrectionAction {

    private static final long serialVersionUID = 1L;

    /* Preconditions */
    
    /** shows in correction menu */
    private boolean inCorrectionMenu;

    /** private company to close */
    private PrivateCompanyI privateCompany;
    
    /** converted to name */
    private String privateCompanyName; 

    /* Postconditions: None */
    
    public ClosePrivate(PrivateCompanyI priv) {
        privateCompany = priv;
        privateCompanyName = priv.getName();
    }
    
    public boolean isInCorrectionMenu(){
        return inCorrectionMenu;
    }
    public void setCorrectionMenu(boolean menu){
        inCorrectionMenu = menu;
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
    public boolean equals(PossibleAction action) {
        if (!(action instanceof ClosePrivate)) return false;
        ClosePrivate a = (ClosePrivate) action;
        return (a.privateCompany == this.privateCompany &&
                a.inCorrectionMenu == this.inCorrectionMenu
        );
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("ClosePrivate");
        if (!acted) {
            b.append(" (not acted)");
            if (privateCompany != null)
                b.append(", privateCompany="+privateCompany);
            b.append(", inCorrectionMenu="+inCorrectionMenu);
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
