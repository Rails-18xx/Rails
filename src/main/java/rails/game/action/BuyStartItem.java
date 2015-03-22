package rails.game.action;

import java.util.SortedSet;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Added updated equals and toString methods
 */
public class BuyStartItem extends StartItemAction {

    /* Server-provided fields */
    private int price;
    private boolean selected;
    protected boolean sharePriceToSet = false;
    protected String companyNeedingSharePrice = null;
    private boolean setSharePriceOnly = false;
    private SortedSet<String> startSpaces = null;

    // Client-provided fields
    private int associatedSharePrice;

    public static final long serialVersionUID = 1L;

    /**
     * 
     * Rails 2.0: Added updated equals methods
     */
    public BuyStartItem(StartItem startItem, int price, boolean selected,
            boolean setSharePriceOnly) {

        super(startItem);
        this.price = price;
        this.selected = selected;
        this.setSharePriceOnly = setSharePriceOnly;

        PublicCompany company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getId();
        }
    }

    public BuyStartItem(StartItem startItem, int price, boolean selected) {

        this(startItem, price, selected, false);
    }

    public int getPrice() {
        return price;
    }

    public boolean isSelected() {
        return selected;
    }

    public int getAssociatedSharePrice() {
        return associatedSharePrice;
    }

    public void setAssociatedSharePrice(int sharePrice) {
        this.associatedSharePrice = sharePrice;
    }

    public boolean hasSharePriceToSet() {
        return sharePriceToSet;
    }

    public boolean setSharePriceOnly() {
        return setSharePriceOnly;
    }

    public String getCompanyToSetPriceFor() {
        return companyNeedingSharePrice;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        // super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        BuyStartItem action = (BuyStartItem) pa;
        boolean options =
                Objects.equal(this.price, action.price)
                        && Objects.equal(this.selected, action.selected)
                        && Objects.equal(this.setSharePriceOnly,
                                action.setSharePriceOnly)
                        && Objects.equal(this.sharePriceToSet,
                                action.sharePriceToSet)
                        && Objects.equal(this.companyNeedingSharePrice,
                                action.companyNeedingSharePrice);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
               && Objects.equal(this.associatedSharePrice,
                       action.associatedSharePrice);
    }

    public String toString() {
        return super.toString()
               + RailsObjects.stringHelper(this).addToString("price", price).addToString(
                       "selected", selected).addToString("setSharePriceOnly",
                       setSharePriceOnly).addToString("sharePriceToSet",
                       sharePriceToSet).addToString("companyNeedingSharePrice",
                       companyNeedingSharePrice).addToStringOnlyActed(
                       "associatedSharePrice", associatedSharePrice).toString();
    }

    public boolean containsStartSpaces() {
        if (startSpaces == null) {
            return false;
        } else {
            return true;
        }
    }

    public SortedSet<String> startSpaces() {
        return startSpaces;
    }

    public void setStartSpaces(SortedSet<String> startSpaces) {
        this.startSpaces = startSpaces;
    }

}
