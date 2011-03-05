/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/TakeLoans.java,v 1.2 2009/10/29 19:41:29 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.PublicCompanyI;

/**
 * @author Erik Vos
 */
public class TakeLoans extends PossibleORAction {

    // Initial attributes
    transient private PublicCompanyI company;
    private String companyName;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberTaken = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public TakeLoans(PublicCompanyI company, int maxNumber,
            int price) {

        this.company = company;
        this.companyName = company.getName();
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
    public PublicCompanyI getCompany() {
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
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof TakeLoans)) return false;
        TakeLoans a = (TakeLoans) action;
        return a.company == company
               && a.maxNumber == maxNumber
               && a.price == price;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof TakeLoans)) return false;
        TakeLoans a = (TakeLoans) action;
        return a.company == company
               && a.numberTaken == numberTaken
               && a.price == price;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append ("TakeLoans ").append(company.getName())
         .append(" maxNumber=").append(maxNumber)
         .append(" value=").append(price);
        if (numberTaken != 0) {
            b.append(" numberTaken="+numberTaken);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        company =
                getCompanyManager().getPublicCompany(companyName);
    }

}
