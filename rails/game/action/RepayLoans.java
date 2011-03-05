/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/RepayLoans.java,v 1.5 2009/10/29 19:41:30 evos Exp $
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
public class RepayLoans extends PossibleAction {

    // Initial attributes
    transient private PublicCompanyI company;
    private String companyName;
    private int minNumber;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberRepaid = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public RepayLoans(PublicCompanyI company, int minNumber, int maxNumber,
            int price) {

        this.company = company;
        this.companyName = company.getName();
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
    public PublicCompanyI getCompany() {
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
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof RepayLoans)) return false;
        RepayLoans a = (RepayLoans) action;
        return a.company == company
               && a.minNumber == minNumber
               && a.maxNumber == maxNumber
               && a.price == price;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof RepayLoans)) return false;
        RepayLoans a = (RepayLoans) action;
        return a.company == company
               && a.numberRepaid == numberRepaid
               && a.price == price;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append ("RepayLoans ").append(company.getName())
         .append(" minNumber=").append(minNumber)
         .append(" maxNumber=").append(maxNumber)
         .append(" value=").append(price);
        if (acted) {
            b.append(" numberRepaid="+numberRepaid);
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
