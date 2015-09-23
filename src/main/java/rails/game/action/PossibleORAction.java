package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 * 
 * Rails 2.0: Added updated equals and toString methods 
 */
public abstract class PossibleORAction extends PossibleAction {

    // Rails 2.0: This is a fix to be compatible with Rails 1.x
    private static final long serialVersionUID = -1656570654856705840L;

    transient protected PublicCompany company;
    protected String companyName;

    /**
     *
     */
    public PossibleORAction() {
        super(null); // not defined by an activity yet
        // TODO: The company field should be set from outside and not inside the action classes themselves
        RoundFacade round = getRoot().getGameManager().getCurrentRound();
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
    
    /**
     * @return costs of executing the action, default for an ORAction is zero
     */
    public int getCost() {
        return 0;
    }

    /** To be used in the client (to enable safety check in the server) */
    public void setCompany(PublicCompany company) {
        this.company = company;
        this.companyName = company.getId();
    }
    
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        PossibleORAction action = (PossibleORAction)pa; 
        return Objects.equal(this.company, action.company);
        // no asAction attributes to be checked
    }
    
    @Override
    public String toString () {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToString("company", company)
                .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }
}
