package rails.game.action;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;

/**
 * @author Erik Vos
 * 
 * Rails 2.0: Added updated equals and toString methods 
 */
public class BuyStartItem extends StartItemAction {

    /* Server-provided fields */
    private int price;
    private boolean selected;
    protected boolean sharePriceToSet = false;
    protected String companyNeedingSharePrice = null;
    private boolean setSharePriceOnly = false;

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
    public boolean equalsAsOption(PossibleAction pa) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAsOption(pa)) return false; 

        // check further attributes
        BuyStartItem action = (BuyStartItem)pa;
        return Objects.equal(this.price, action.price)
                && Objects.equal(this.selected, action.selected)
                && Objects.equal(this.setSharePriceOnly, action.setSharePriceOnly)
                && Objects.equal(this.sharePriceToSet, action.sharePriceToSet)
                && Objects.equal(this.companyNeedingSharePrice, action.companyNeedingSharePrice)
        ;
    }
    
    @Override
    public boolean equalsAsAction (PossibleAction pa) {
        // first check if equal as option
        if (!this.equalsAsOption(pa)) return false;
        
        // check further attributes
        BuyStartItem action = (BuyStartItem)pa; 
        return Objects.equal(this.associatedSharePrice, action.associatedSharePrice);
    }

   public String toString() {
       return super.toString() + 
               RailsObjects.stringHelper(this)
               .addToString("price", price)
               .addToString("selected", selected)
               .addToString("setSharePriceOnly", setSharePriceOnly)
               .addToString("sharePriceToSet", sharePriceToSet)
               .addToString("companyNeedingSharePrice", companyNeedingSharePrice)
               .addToStringOnlyActed("associatedSharePrice", associatedSharePrice)
               .toString()
      ;
   } 

}
