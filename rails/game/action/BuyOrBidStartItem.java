/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyOrBidStartItem.java,v 1.10 2008/06/04 19:00:29 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.PublicCompanyI;
import rails.game.StartItem;

/**
 * @author Erik Vos
 */
public class BuyOrBidStartItem extends PossibleAction {

    /* Server-provided fields */
    transient private StartItem startItem;
    private String startItemName;
    private boolean sharePriceToSet = false;
    private String companyNeedingSharePrice = null;
    /**
     * Selectable means that a player can select but not buy it. In this case
     * "Pass" means "Select but no buy".
     */
    private boolean selectable = false;
    private int type;

    /*
     * Status of the start item (buyable? biddable?) for the <i>current</i>
     * player, taking into account the amount of cash of this player that is
     * blocked by bids on other items.
     */
    private int priceOrMinimumBid;
    private int bidIncrement;
    private int itemIndex;

    /* Client-provided fields */
    private int actualBid = 0;
    private int sharePrice = 0;

    // Constants
    public static final int BUY_IMMEDIATE = 1;
    public static final int SELECT_AND_BUY = 2;
    public static final int BID_IMMEDIATE = 3;
    public static final int SELECT_AND_BID = 4;
    public static final int SELECT_AND_BID_OR_PASS = 5;
    public static final int SET_SHARE_PRICE = 6;

    public static final String[] typeNames =
            new String[] { "", "BuyImmediate", "SelectAndBuy", "BidImmediate",
                    "SelectAndBid", "SelectAndBidOrPass", "SetSharePrice" };

    public static final long serialVersionUID = 2L;

    /**
     * 
     */
    public BuyOrBidStartItem(StartItem startItem, int priceOrMinimumBid,
            int type) {

        super();
        this.startItem = startItem;
        this.startItemName = startItem.getName();
        this.itemIndex = startItem.getIndex();
        this.priceOrMinimumBid = priceOrMinimumBid;

        setType(type);

        PublicCompanyI company;
        if ((company = startItem.needsPriceSetting()) != null) {
            sharePriceToSet = true;
            companyNeedingSharePrice = company.getName();
        }
    }

    /**
     * @return Returns the startItem.
     */
    public StartItem getStartItem() {
        return startItem;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    /** @deprecated */
    /* ???? */
    public BuyOrBidStartItem setSelectable(boolean value) {
        selectable = value;
        return this;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public int getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(int bidIncrement) {
        this.bidIncrement = bidIncrement;
    }

    public int getActualBid() {
        return actualBid;
    }

    public void setActualBid(int actualBid) {
        this.actualBid = actualBid;
    }

    public int getSharePrice() {
        return sharePrice;
    }

    public void setSharePrice(int sharePrice) {
        this.sharePrice = sharePrice;
    }

    public boolean hasSharePriceToSet() {
        return sharePriceToSet;
    }

    public String getCompanyToSetPriceFor() {
        return companyNeedingSharePrice;
    }

    public int getPriceOrMinimumBid() {
        return priceOrMinimumBid;
    }

    /** @deprecated */
    public int getStatus() {
        // if (startItem == null) return 0;//BAD
        return startItem.getStatus();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean equals(PossibleAction action) {
        if (!(action instanceof BuyOrBidStartItem)) return false;
        BuyOrBidStartItem a = (BuyOrBidStartItem) action;
        return a.startItem == startItem && a.itemIndex == itemIndex
               && a.type == type;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("BuyOrBidStartItem ").append(startItemName).append(" type=").append(
                typeNames[type]).append(" status=");
        if (selectable) b.append("Selectable/");
        b.append(startItem.getStatusName());

        switch (getStatus()) {
        case StartItem.BIDDABLE:
            b.append(" minbid=").append(priceOrMinimumBid).append(" bid=").append(
                    actualBid);
            break;
        case StartItem.BUYABLE:
            b.append(" price=").append(priceOrMinimumBid);
            break;
        case StartItem.NEEDS_SHARE_PRICE:
            b.append(" startprice=").append(sharePrice);
            break;
        }
        return b.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        startItem = StartItem.getByName(startItemName);

    }
}
