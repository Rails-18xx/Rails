/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/BuyPrivate.java,v 1.1 2006/09/17 20:42:50 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package game.action;

import game.PrivateCompanyI;

/**
 * @author Erik Vos
 */
public class BuyPrivate extends PossibleAction {
    
    private PrivateCompanyI privateCompany;
    private int minimumPrice;
    private int maximumPrice;

    /**
     * 
     */
    public BuyPrivate(PrivateCompanyI privateCompany, int minimumPrice, int maximumPrice) {
        this.privateCompany = privateCompany;
        this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice;
    }

    
    /**
     * @return Returns the maximumPrice.
     */
    public int getMaximumPrice() {
        return maximumPrice;
    }
    /**
     * @return Returns the minimumPrice.
     */
    public int getMinimumPrice() {
        return minimumPrice;
    }
    /**
     * @return Returns the privateCompany.
     */
    public PrivateCompanyI getPrivateCompany() {
        return privateCompany;
    }
}
