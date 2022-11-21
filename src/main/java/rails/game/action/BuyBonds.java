package rails.game.action;

import com.google.common.base.Objects;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.util.RailsObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Rails 2.0: Updated equals and toString methods
*/
public class BuyBonds extends PossibleAction {

    /* Server side parameters */
    private transient RailsOwner from;
    private String fromId;
    private transient RailsOwner to;
    private String toId;
    private transient PublicCompany company;
    private String companyId;
    private int price;
    private int maxNumber;


    /* Client side parameter */
    private int numberBought = 0;

    public static final long serialVersionUID = 1L;

    //private static final Logger log = LoggerFactory.getLogger(BuyBonds.class);

    public BuyBonds(RailsOwner from, RailsOwner to, PublicCompany company, int maxNumber, int price) {
        super(from.getRoot());

        /* Server side parameters */
        this.from = from;
        this.fromId = from.getId();
        this.to = to;
        this.toId = to.getId();
        this.company = company;
        this.companyId = company.getId();
        this.price = price;
        this.maxNumber = maxNumber;
    }

    public void setNumberBought(int numberBought) {
        this.numberBought = numberBought;
    }

    public RailsOwner getFrom() {
        return from;
    }

    public String getFromId() {
        return fromId;
    }

    public RailsOwner getTo() {
        return to;
    }

    public String getToId() {
        return toId;
    }

    public PublicCompany getCompany() {
        return company;
    }

    public String getCompanyId() {
        return companyId;
    }

    public int getMaxNumber() {
        return maxNumber;
    }

    public int getNumberBought() {
        return numberBought;
    }

    public int getPrice() {
        return price;
    }

    // Two more setters on behalf of ListAndFixSavedFiles

    public void setPrice(int price) {
        this.price = price;
    }

    public void setMaxNumber(int maxNumber) {
        this.maxNumber = maxNumber;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        BuyBonds action = (BuyBonds)pa;
        boolean options =  Objects.equal(this.from, action.from)
                && Objects.equal(this.to, action.to)
                && Objects.equal(this.price, action.price)
                && Objects.equal(this.company, action.company)
                && Objects.equal(this.maxNumber, action.maxNumber);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes (none)
        return options;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("company", company)
                    .addToString("from", from)
                    .addToString("to", to)
                    .addToString("price", price)
                    .addToString("max", maxNumber)
                    .addToStringOnlyActed("bought", numberBought)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        from = root.getPortfolioManager().getPortfolioByName(fromId).getParent();
        to = root.getPortfolioManager().getPortfolioByName(toId).getParent();
        company = root.getCompanyManager().getPublicCompany(companyId);
    }

}
