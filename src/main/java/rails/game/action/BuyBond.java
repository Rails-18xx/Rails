package rails.game.action;

import com.google.common.base.Objects;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.util.RailsObjects;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Rails 2.0: Updated equals and toString methods
*/
public class BuyBond extends PossibleAction {

    // Initial attributes
    private transient RailsOwner from;
    private String fromId;
    private transient RailsOwner to;
    private String toId;
    private transient PublicCompany company;
    private String companyId;
    private int price;

    // No user-assigned attributes

    public static final long serialVersionUID = 1L;

    public BuyBond(RailsOwner from, RailsOwner to, PublicCompany company, int price) {
        super(from.getRoot());
        this.from = from;
        this.fromId = from.getId();
        this.to = to;
        this.toId = to.getId();
        this.company = company;
        this.companyId = company.getId();
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        BuyBond action = (BuyBond)pa;
        boolean options =  Objects.equal(this.from, action.from)
                && Objects.equal(this.to, action.to)
                && Objects.equal(this.price, action.price);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes (none)
        return options;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("from", from)
                    .addToString("to", to)
                    .addToString("price", price)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        from = root.getPortfolioManager().getPortfolioByUniqueName(fromId).getParent();
        to = root.getPortfolioManager().getPortfolioByUniqueName(toId).getParent();
        company = root.getCompanyManager().getPublicCompany(companyId);
    }

}
