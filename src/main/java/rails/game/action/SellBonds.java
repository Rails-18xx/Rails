package rails.game.action;

import com.google.common.base.Objects;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;

import java.io.IOException;
import java.io.ObjectInputStream;

public class SellBonds extends PossibleAction {

    /* Server side parameters */
    private transient PublicCompany company;
    private String companyId;
    private int price;
    private int maxNumber;

    /* Client side parameter */
    private int numberSold = 0;

    public SellBonds (PublicCompany company, int price, int maxNumber) {
        super(company.getRoot());

        this.company = company;
        this.companyId = company.getId();
        this.price = price;
        this.maxNumber = maxNumber;
    }

    public PublicCompany getCompany() {
        return company;
    }

    public String getCompanyId() {
        return companyId;
    }

    public int getPrice() {
        return price;
    }

    public int getMaxNumber() {
        return maxNumber;
    }

    public int getNumberSold() {
        return numberSold;
    }

    public void setNumberSold(int numberSold) {
        this.numberSold = numberSold;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        SellBonds action = (SellBonds)pa;
        boolean options = Objects.equal(this.price, action.price)
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
                        .addToString("price", price)
                        .addToString("max", maxNumber)
                        .addToStringOnlyActed("sold", numberSold)
                        .toString()
                ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        company = root.getCompanyManager().getPublicCompany(companyId);
    }


}
