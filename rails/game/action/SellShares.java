/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/SellShares.java,v 1.6 2009/10/29 19:41:29 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.Bank;
import rails.game.PublicCompanyI;
import rails.util.Util;

/**
 * @author Erik Vos
 */
public class SellShares extends PossibleAction {

    // Server-side settings
    private String companyName;
    transient private PublicCompanyI company;
    private int shareUnit;
    private int shareUnits;
    private int share;
    private int price;
    private int maximumNumber;

    // Client-side settings
    private int numberSold = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public SellShares(String companyName, int shareUnits, int maximumNumber,
            int price) {
        this.companyName = companyName;
        this.shareUnits = shareUnits;
        this.price = price;
        this.maximumNumber = maximumNumber;

        company = getCompanyManager().getPublicCompany(companyName);
        shareUnit = company.getShareUnit();
        share = shareUnits * shareUnit;
    }

    /**
     * @return Returns the maximumNumber.
     */
    public int getMaximumNumber() {
        return maximumNumber;
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

    public PublicCompanyI getCompany() {
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

    public int getNumberSold() {
        return numberSold;
    }

    public void setNumberSold(int numberSold) {
        this.numberSold = numberSold;
    }

    @Override
	public boolean equals(PossibleAction action) {
        if (!(action instanceof SellShares)) return false;
        SellShares a = (SellShares) action;
        return a.getCompanyName().equals(companyName)
               && a.getShareUnits() == shareUnits
               && a.getMaximumNumber() == maximumNumber
               && a.getPrice() == price;
    }

    @Override
	public String toString() {
        return "SellShares: "
               + (numberSold > 0 ? numberSold : "max " + maximumNumber)
               + " of " + share + "% " + companyName + " at "
               + Bank.format(shareUnits * price) + " apiece";
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }
}
