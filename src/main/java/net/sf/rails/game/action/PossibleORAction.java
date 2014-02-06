package net.sf.rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.rails.game.*;
import net.sf.rails.util.Util;


/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 *
 * @author Erik Vos
 */
/* Or should this be an interface? We will see. */
public abstract class PossibleORAction extends PossibleAction {

    // This is a fix to be compatible with Rails 1.x
    private static final long serialVersionUID = -1656570654856705840L;

    transient protected PublicCompany company;
    protected String companyName;

    /**
     *
     */
    public PossibleORAction() {

        super();
        Round round = GameManager.getInstance().getCurrentRound();
        if (round instanceof OperatingRound) {
            company = ((OperatingRound) round).getOperatingCompany();
            companyName = company.getId();
        }
    }

    public PublicCompany getCompany() {
        return company;
    }

    public String getCompanyName() {
        return company.getId();
    }

    /** To be used in the client (to enable safety check in the server) */
    public void setCompany(PublicCompany company) {
        this.company = company;
        this.companyName = company.getId();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }
}
