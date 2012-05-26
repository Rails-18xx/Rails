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

    // FIXME: Does this still work?, added to avoid warning
    private static final long serialVersionUID = 1L;

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
