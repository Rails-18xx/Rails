/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyBonusToken.java,v 1.5 2010/02/28 21:38:06 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.CashHolder;
import rails.game.PrivateCompanyI;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialProperty;

/**
 * @author Erik Vos
 */
public class BuyBonusToken extends PossibleORAction {

    // Initial attributes
    transient private PrivateCompanyI privateCompany;
    private String privateCompanyName;
    transient private CashHolder seller = null;
    private String sellerName = null;
    transient protected SellBonusToken specialProperty = null;
    protected int specialPropertyId;

    private String name;
    private int price;
    private int value;
    private String locationString;

    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public BuyBonusToken(SellBonusToken specialProperty) {

        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
        this.privateCompany = (PrivateCompanyI) specialProperty.getOriginalCompany();
        this.privateCompanyName = privateCompany.getName();
        this.seller = specialProperty.getSeller();
        if (seller != null) this.sellerName = seller.getName();
        this.name = specialProperty.getName();
        this.price = specialProperty.getPrice();
        this.value = specialProperty.getValue();
        this.locationString = specialProperty.getLocationNameString();
    }

    /**
     * @return Returns the privateCompany.
     */
    public PrivateCompanyI getPrivateCompany() {
        return privateCompany;
    }

    public String getPrivateCompanyName() {
        return privateCompanyName;
    }

    public CashHolder getSeller() {
        return seller;
    }

    public String getSellerName() {
        return sellerName;
    }

    public SellBonusToken getSpecialProperty() {
        return specialProperty;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getLocationString() {
        return locationString;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof BuyBonusToken)) return false;
        BuyBonusToken a = (BuyBonusToken) action;
        return a.privateCompany == privateCompany
                && a.name.equals(name)
                && a.price == price
                && a.value == value
                && a.locationString.equals(locationString);
    }

    public boolean equalsAsAction(PossibleAction action) {
        return action.equalsAsOption (this);
    }
    
    @Override
    public String toString() {
        return "BuyBonusToken " + privateCompanyName
                + " owner=" + sellerName
                + " price=" + price
                + " value=" + value
                + " locations=" + locationString;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        privateCompany =
                getCompanyManager().getPrivateCompany(privateCompanyName);
        if (sellerName.equalsIgnoreCase("Bank")) {
            seller = gameManager.getBank();
        } else if (sellerName != null) {
            seller =
                getCompanyManager().getPublicCompany(sellerName);
        }
        if (specialPropertyId > 0) {
            specialProperty =
                    (SellBonusToken) SpecialProperty.getByUniqueId(specialPropertyId);
        }
    }

}
