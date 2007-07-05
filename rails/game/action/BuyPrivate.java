/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyPrivate.java,v 1.2 2007/07/05 17:57:54 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.PrivateCompanyI;

/**
 * @author Erik Vos
 */
public class BuyPrivate extends PossibleORAction {
    
    // Initial attributes
    private PrivateCompanyI privateCompany;
    private int minimumPrice;
    private int maximumPrice;
    
    // User-assigned attributes
    private int price = 0;

    /**
     * 
     */
    public BuyPrivate(PrivateCompanyI privateCompany, 
            int minimumPrice, int maximumPrice) {
        
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
    
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean equals (PossibleAction action) {
        if (!(action instanceof BuyPrivate)) return false;
        BuyPrivate a = (BuyPrivate) action;
        return a.privateCompany == privateCompany
            && a.minimumPrice == minimumPrice
            && a.maximumPrice == maximumPrice;
    }

    public String toString() {
        return "BuyPrivate "+ privateCompany.getName() 
        	+ " holder=" + privateCompany.getPortfolio().getName();
    }

}
