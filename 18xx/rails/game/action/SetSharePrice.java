/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/SetSharePrice.java,v 1.2 2008/06/04 19:00:29 evos Exp $
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
public class SetSharePrice extends StartItemAction {

    /* Server-provided fields */
    private boolean sharePriceToSet = false;
    private String companyNeedingSharePrice = null;

    // Client-provided fields
    private int associatedSharePrice;

    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public SetSharePrice(StartItem startItem) {

        super(startItem);

        PublicCompanyI company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getName();
        }
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

    public String getCompanyToSetPriceFor() {
        return companyNeedingSharePrice;
    }

    /** @deprecated */
    public int getStatus() {
        // if (startItem == null) return 0;//BAD
        return startItem.getStatus();
    }

    public boolean equals(PossibleAction action) {
        if (!(action instanceof SetSharePrice)) return false;
        SetSharePrice a = (SetSharePrice) action;
        return a.startItem == startItem && a.itemIndex == itemIndex;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("SetSharePrice ").append(startItemName).append(" shareprice=").append(
                associatedSharePrice).append(
                " for company " + companyNeedingSharePrice);
        return b.toString();
    }

}
