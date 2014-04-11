package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.*;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;


/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 *
 * @author Erik Vos
 * 
 * Rails 2.0: Added updated equals and toString methods 
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
    
    
    @Override
    public boolean equalsAsOption (PossibleAction pa) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAsOption(pa)) return false; 

        // check further attributes
        PossibleORAction action = (PossibleORAction)pa; 
        return Objects.equal(this.company, action.company);
    }
    
    @Override
    public boolean equalsAsAction (PossibleAction pa) {
        // no further test compared to option
        return this.equalsAsOption(pa);
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
