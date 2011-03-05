/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyStartItem.java,v 1.4 2008/12/03 20:15:15 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.PublicCompanyI;
import rails.game.StartItem;

/**
 * @author Erik Vos
 */
public class BuyStartItem extends StartItemAction {

    /* Server-provided fields */
    private int price;
    private boolean selected;
    private boolean sharePriceToSet = false;
    private String companyNeedingSharePrice = null;
    private boolean setSharePriceOnly = false;

    // Client-provided fields
    private int associatedSharePrice;

    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public BuyStartItem(StartItem startItem, int price, boolean selected,
            boolean setSharePriceOnly) {

        super(startItem);
        this.price = price;
        this.selected = selected;
        this.setSharePriceOnly = setSharePriceOnly;

        PublicCompanyI company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getName();
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

    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof BuyStartItem)) return false;
        BuyStartItem a = (BuyStartItem) action;
        return a.startItem == startItem && a.itemIndex == itemIndex
               && a.price == price;
    }

    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof BuyStartItem)) return false;
        BuyStartItem a = (BuyStartItem) action;
        return a.equalsAsOption(this)
               && a.associatedSharePrice == associatedSharePrice;
    }

   public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("BuyStartItem ").append(startItemName).append(" price=").append(
                price).append(" selected=").append(selected);

        if (sharePriceToSet) {
            b.append(" shareprice=").append(associatedSharePrice).append(
                    " for company " + companyNeedingSharePrice);
        }
        return b.toString();
    }

}
