/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyPrivate.java,v 1.7 2010/01/31 22:22:28 macfreek Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.PrivateCompany;

/**
 * @author Erik Vos
 */
public class BuyPrivate extends PossibleORAction {

    // Initial attributes
    transient private PrivateCompany privateCompany;
    private String privateCompanyName;
    private int minimumPrice;
    private int maximumPrice;

    // User-assigned attributes
    private int price = 0;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public BuyPrivate(PrivateCompany privateCompany, int minimumPrice,
            int maximumPrice) {

        this.privateCompany = privateCompany;
        this.privateCompanyName = privateCompany.getId();
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
    public PrivateCompany getPrivateCompany() {
        return privateCompany;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof BuyPrivate)) return false;
        BuyPrivate a = (BuyPrivate) action;
        return a.privateCompany == privateCompany
               && a.minimumPrice == minimumPrice
               && a.maximumPrice == maximumPrice;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof BuyPrivate)) return false;
        BuyPrivate a = (BuyPrivate) action;
        return a.privateCompany == privateCompany
               && a.price == price;
    }

    @Override
    public String toString() {
        return "BuyPrivate " + privateCompany.getId() + " holder="
               + privateCompany.getPortfolio().getId();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        privateCompany =
                getCompanyManager().getPrivateCompany(privateCompanyName);
    }

}
