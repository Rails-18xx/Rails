package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: updated equals and toString methods
 */
public class RepayLoans extends PossibleAction {

    // Initial attributes
    transient private PublicCompany company;
    private String companyName;
    private int minNumber;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberRepaid = 0;

    public static final long serialVersionUID = 1L;

    public RepayLoans(PublicCompany company, int minNumber, int maxNumber,
            int price) {
        super(null); // not defined by an activity yet
        this.company = company;
        this.companyName = company.getId();
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        this.price = price;
    }

    public int getMinNumber() {
        return minNumber;
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
    public PublicCompany getCompany() {
        return company;
    }

    /**
     * @return Returns the company.
     */
    public String getCompanyName() {
        return companyName;
    }

    public int getPrice() {
        return price;
    }

    public void setNumberTaken(int numberRepaid) {
        this.numberRepaid = numberRepaid;
    }

    public int getNumberRepaid() {
        return numberRepaid;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        RepayLoans action = (RepayLoans) pa;
        boolean options = 
                Objects.equal(this.company, action.company)
                && Objects.equal(this.minNumber, action.minNumber)
                && Objects.equal(this.maxNumber, action.maxNumber)
                && Objects.equal(this.price, action.price)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.numberRepaid, action.numberRepaid)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("company", company)
                    .addToString("minNumber", minNumber)
                    .addToString("maxNumber", maxNumber)
                    .addToString("price", price)
                    .addToStringOnlyActed("numberRepaid", numberRepaid)
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
