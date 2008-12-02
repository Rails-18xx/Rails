/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BidStartItem.java,v 1.3 2008/12/02 20:29:15 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.StartItem;

/**
 * @author Erik Vos
 */
public class BidStartItem extends StartItemAction {

    /* Server-provided fields */
    private int minimumBid;
    private int bidIncrement;
    private boolean selected;
    private boolean selectForAuction;

    /* Client-provided fields */
    private int actualBid = 0;

    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
            boolean selected, boolean selectForAuction) {

        super(startItem);
        this.minimumBid = minimumBid;
        this.bidIncrement = bidIncrement;
        this.selected = selected;
        this.selectForAuction = selectForAuction;

    }

    public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
            boolean selected) {

        this(startItem, minimumBid, bidIncrement, selected, false);

    }

    public int getMinimumBid() {
        return minimumBid;
    }

    public int getBidIncrement() {
        return bidIncrement;
    }

    public int getActualBid() {
        return actualBid;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isSelectForAuction() {
        return selectForAuction;
    }

    public void setActualBid(int actualBid) {
        this.actualBid = actualBid;
    }

    public int getStatus() {
        return startItem.getStatus();
    }

    public boolean equals(PossibleAction action) {
        if (!(action instanceof BidStartItem)) return false;
        BidStartItem a = (BidStartItem) action;
        return a.startItem == startItem && a.itemIndex == itemIndex
               && a.minimumBid == minimumBid;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("BidStartItem ").append(startItemName).append(" minbid=").append(
                minimumBid).append(" selected=").append(selected).append(
                " selectForAuction=").append(selectForAuction).append(" bid=").append(
                actualBid);
        return b.toString();
    }

}
