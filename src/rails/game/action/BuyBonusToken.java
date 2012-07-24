package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.PrivateCompany;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialProperty;
import rails.game.state.Owner;

/**
 * @author Erik Vos
 */
public class BuyBonusToken extends PossibleORAction {

    // Initial attributes
    transient private PrivateCompany privateCompany;
    private String privateCompanyName;
    transient private Owner seller = null;
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
        this.privateCompany = (PrivateCompany) specialProperty.getOriginalCompany();
        this.privateCompanyName = privateCompany.getId();
        this.seller = specialProperty.getSeller();
        if (seller != null) this.sellerName = seller.getId();
        this.name = specialProperty.getId();
        this.price = specialProperty.getPrice();
        this.value = specialProperty.getValue();
        this.locationString = specialProperty.getLocationNameString();
    }

    /**
     * @return Returns the privateCompany.
     */
    public PrivateCompany getPrivateCompany() {
        return privateCompany;
    }

    public String getPrivateCompanyName() {
        return privateCompanyName;
    }

    public Owner getSeller() {
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
            // TODO: Assume that it is the pool, not the ipo
            seller = gameManager.getBank().getPool();
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
