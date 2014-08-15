package rails.game.action;

import java.util.Arrays;

import com.google.common.base.Objects;

import net.sf.rails.game.*;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class StartCompany extends BuyCertificate {

    // Server parameters
    protected int[] startPrices;

    public static final long serialVersionUID = 1L;

    public StartCompany(PublicCompany company, int[] prices,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                RailsRoot.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.startPrices = prices.clone();
    }

    public StartCompany(PublicCompany company, int[] startPrice) {
        this(company, startPrice, 1);
    }

    public StartCompany(PublicCompany company, int price,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                RailsRoot.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.price = price;
    }

    public StartCompany(PublicCompany company, int price) {
        this(company, price, 1);
    }

    public int[] getStartPrices() {
        return startPrices;
    }

    public boolean mustSelectAPrice() {
        return startPrices != null/* && startPrices.length > 1*/;
    }

    public void setStartPrice(int startPrice) {
        price = startPrice;
    }

    // FIXME: Attribute price of BuyCertificate now mutable, instead of static
    // Consider changing the class hierarchy, currently price in BuyCertificate is not checked
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        StartCompany action = (StartCompany)pa; 
        boolean options = 
                Arrays.equals(this.startPrices, action.startPrices)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.price, action.price)
        // TODO: price has to be checked here, as this cannot be done in BuyCertificate
        ;
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("startPrices", Arrays.toString(startPrices))
                    .toString()
        ;
    }

}
