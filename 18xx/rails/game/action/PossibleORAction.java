/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleORAction.java,v 1.4 2009/10/29 19:41:29 evos Exp $
 *
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.*;
import rails.util.Util;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 *
 * @author Erik Vos
 */
/* Or should this be an interface? We will see. */
public abstract class PossibleORAction extends PossibleAction {

    transient protected PublicCompanyI company;
    protected String companyName;

    /**
     *
     */
    public PossibleORAction() {

        super();
        RoundI round = GameManager.getInstance().getCurrentRound();
        if (round instanceof OperatingRound) {
            company = ((OperatingRound) round).getOperatingCompany();
            companyName = company.getName();
        }
    }

    public PublicCompanyI getCompany() {
        return company;
    }

    public String getCompanyName() {
        return company.getName();
    }

    /** To be used in the client (to enable safety check in the server) */
    public void setCompany(PublicCompanyI company) {
        this.company = company;
        this.companyName = company.getName();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }
}
