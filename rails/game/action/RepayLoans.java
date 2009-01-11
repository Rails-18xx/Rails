/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/RepayLoans.java,v 1.1 2009/01/11 17:24:46 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.Game;
import rails.game.PublicCompanyI;

/**
 * @author Erik Vos
 */
public class RepayLoans extends PossibleORAction {

    // Initial attributes
    transient private PublicCompanyI company;
    private String companyName;
    private int maxNumber;
    private int price;

    // User-assigned attributes
    private int numberRepaid = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public RepayLoans(PublicCompanyI company, int maxNumber,
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

    public void setNumberTaken(int numberRepaid) {
        this.numberRepaid = numberRepaid;
    }

    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof RepayLoans)) return false;
        RepayLoans a = (RepayLoans) action;
        return a.company == company
               && a.maxNumber == maxNumber
               && a.price == price;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append ("RepayLoans ").append(company.getName())
         .append(" maxNumber=").append(maxNumber)
         .append(" value=").append(price);
        if (numberRepaid != 0) {
            b.append(" numberRepaid="+numberRepaid);
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        company =
                Game.getCompanyManager().getPublicCompany(companyName);
    }

}
