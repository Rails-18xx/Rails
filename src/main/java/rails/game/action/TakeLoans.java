package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class TakeLoans extends PossibleORAction {

    // Initial attributes
    // TODO: This is a duplication of the field in PossibleORAction
    // Is there a reason for that? (potentially that it could be used outside of ORs)
    transient private PublicCompany company;
    private String companyName;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberTaken = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public TakeLoans(PublicCompany company, int maxNumber,
            int price) {

        this.company = company;
        this.companyName = company.getId();
        this.maxNumber = maxNumber;
        this.price = price;
    }

    /**
     * @return Returns the minimumPrice.
     */
    public int getMaxNumber() {
        return maxNumber;
    }

    /**
     * @return Returns the company.
     */
    @Override
    public PublicCompany getCompany() {
        return company;
    }

    public int getPrice() {
        return price;
    }

    public void setNumberTaken(int numberTaken) {
        this.numberTaken = numberTaken;
    }

    public int getNumberTaken() {
        return numberTaken;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        TakeLoans action = (TakeLoans)pa; 
        boolean options = 
                Objects.equal(this.company, action.company)
                && Objects.equal(this.maxNumber, action.maxNumber)
                && Objects.equal(this.price, action.price)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.numberTaken, action.numberTaken)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("company", company)
                    .addToString("maxNumber", maxNumber)
                    .addToString("price", price)
                    .addToStringOnlyActed("numberTaken", numberTaken)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        company =
                getCompanyManager().getPublicCompany(companyName);
    }

}
