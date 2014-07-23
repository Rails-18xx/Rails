package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
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
        this.name = specialProperty.getName();
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
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        BuyBonusToken action = (BuyBonusToken)pa; 
        return Objects.equal(this.privateCompany, action.privateCompany)
                && Objects.equal(this.name, action.name)
                && Objects.equal(this.price, action.price)
                && Objects.equal(this.value, action.value)
                && Objects.equal(this.locationString, action.locationString)
        ;
        // no asAction attributes to be checked
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("privateCompany", privateCompany)
                    .addToString("name", name)
                    .addToString("price", price)
                    .addToString("value", value)
                    .addToString("locationString", locationString)
                    .toString()
        ;

    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        privateCompany =
                getCompanyManager().getPrivateCompany(privateCompanyName);
        if (sellerName.equalsIgnoreCase("Bank")) {
            // TODO: Assume that it is the pool, not the ipo
            seller = getRoot().getBank().getPool();
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
