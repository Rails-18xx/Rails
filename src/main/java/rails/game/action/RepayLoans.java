package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
// --- START FIX ---
import java.awt.Color;
import net.sf.rails.game.state.Owner;
// --- END FIX ---

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: updated equals and toString methods
 */
// --- START FIX ---
public class RepayLoans extends PossibleAction implements GuiTargetedAction {
// --- END FIX ---

    // Initial attributes
    transient private PublicCompany company;
    private String companyName;
    private int minNumber;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberRepaid = 0;
    private String customLabel = null;

    public void setCustomLabel(String label) {
        this.customLabel = label;
    }

    public static final long serialVersionUID = 1L;

    public RepayLoans(PublicCompany company, int minNumber, int maxNumber,
            int price) {
        super(company.getRoot()); // not defined by an activity yet
        this.company = company;
        this.companyName = company.getId();
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        this.price = price;
    }

    // --- START FIX ---
    @Override
    public Owner getActor() {
        return getCompany();
    }

    @Override
    public String getGroupLabel() {
        return "Forced Loan Repayment";
    }

    @Override
    public String getButtonLabel() {
if (customLabel != null) return customLabel;
        return "Repay Loans ($" + price + " each)";
        }


        @Override
    public Color getButtonColor() {
        return null; 
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return null;
    }

    @Override
    public Color getHighlightBorderColor() {
        return null; 
    }

    @Override
    public Color getHighlightTextColor() {
        return null;
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
        if (company == null && companyName != null && getRoot() != null) {
            company = getRoot().getCompanyManager().getPublicCompany(companyName);
        }
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
                && Objects.equal(this.customLabel, action.customLabel)
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
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        company = getCompanyManager().getPublicCompany(companyName);
    }

}