package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
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
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        BuyPrivate action = (BuyPrivate)pa; 
        boolean options =  Objects.equal(this.privateCompany, action.privateCompany)
                && Objects.equal(this.minimumPrice, action.minimumPrice)
                && Objects.equal(this.maximumPrice, action.maximumPrice)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.price, action.price)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("privateCompany", privateCompany)
                    .addToString("minimumPrice", minimumPrice)
                    .addToString("maximumPrice", maximumPrice)
                    .addToStringOnlyActed("price", price)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        privateCompany =
                getCompanyManager().getPrivateCompany(privateCompanyName);
    }

}
