package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class SellShares extends PossibleAction {

    // Server-side settings
    private String companyName;
    transient private PublicCompany company;
    private int shareUnit;
    private int shareUnits;
    private int share;
    private int price;
    private int number;
    /** Dump flag, indicates to which type of certificates the president's share must be exchanged.<br>
     * 0 = no dump, or dump that does not require any choice of exchange certificates;<br>
     * 1 = exchange against 1-share certificates (usually 10%);<br>
     * 2 = exchange against a 2-share certificate (as can occur in 1835);<br>
     * etc.
     */
    private int presidentExchange = 0;

    // For backwards compatibility only
    private int numberSold = 0;

    public static final long serialVersionUID = 1L;

    public SellShares(PublicCompany company, int shareUnits, int number,
            int price) {
        this (company, shareUnits, number, price, 0);
    }

    public SellShares(PublicCompany company, int shareUnits, int number,
            int price, int presidentExchange) {
        super(null); // not defined by an activity yet
        this.company = company;
        this.shareUnits = shareUnits;
        this.price = price;
        this.number = number;
        this.presidentExchange = presidentExchange;

        companyName = company.getId();
        shareUnit = company.getShareUnit();
        share = shareUnits * shareUnit;
    }

    /**
     * @return Returns the maximumNumber.
     */
    public int getNumber() {
        return number;
    }

    /**
     * @return Returns the price.
     */
    public int getPrice() {
        return price;
    }

    /**
     * @return Returns the companyName.
     */
    public String getCompanyName() {
        return companyName;
    }

    public PublicCompany getCompany() {
        return getCompanyManager().getPublicCompany(companyName);
    }

    public int getShareUnits() {
        return shareUnits;
    }

    public int getShareUnit() {
        return shareUnit;
    }

    public int getShare() {
        return share;
    }

    public int getPresidentExchange() {
        return presidentExchange;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        SellShares action = (SellShares) pa;

        return Objects.equal(this.company, action.company)
                && Objects.equal(this.shareUnit, action.shareUnit)
                && Objects.equal(this.shareUnits, action.shareUnits)
                && Objects.equal(this.share, action.share)
                && Objects.equal(this.price, action.price)
                && Objects.equal(this.number, action.number)
        //        && Objects.equal(this.presidentExchange, action.presidentExchange)
        ;
        // no asAction attributes to be checked
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("company", company)
                    .addToString("shareUnit", shareUnit)
                    .addToString("shareUnits", shareUnits)
                    .addToString("share", share)
                    .addToString("price", price)
                    .addToString("number", number)
                    .addToString("presidentExchange", presidentExchange)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        //in.defaultReadObject();
        // Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();

        companyName = (String) fields.get("companyName", null);
        shareUnit = fields.get("shareUnit", shareUnit);
        shareUnits = fields.get("shareUnits", shareUnits);
        share = fields.get("share", share);
        price = fields.get("price", price);
        numberSold = fields.get("numberSold", 0); // For backwards compatibility
        number = fields.get("number", numberSold);
        presidentExchange = fields.get("presidentExchange", 0);

        CompanyManager companyManager = getCompanyManager();
        if (Util.hasValue(companyName))
            companyName = companyManager.checkAlias(companyName);
        company = companyManager.getPublicCompany(companyName);
    }
}
