/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyOrBidStartItem.java,v 1.6 2007/12/30 14:25:12 evos Exp $
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
    
    /* Status of the start item (buyable? biddable?) for the
     * <i>current</i> player, taking into account the amount of
     * cash of this player that is blocked by bids on other items.
     */
    private int priceOrMinimumBid;
    private int itemIndex;
    
    /* Client-provided fields */
    private int actualBid = 0;
    private int sharePrice = 0;
    
    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public BuyOrBidStartItem(StartItem startItem, 
    		int priceOrMinimumBid, int status) {
    	this (startItem, priceOrMinimumBid);
        this.startItem.setStatus(status);
    }

    public BuyOrBidStartItem(StartItem startItem, 
            int priceOrMinimumBid) {
        super();
        this.startItem = startItem;
        this.startItemName = startItem.getName();
        this.itemIndex = startItem.getIndex();
        this.priceOrMinimumBid = priceOrMinimumBid;
        
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
	
	public int getStatus() {
        return startItem.getStatus();
	}
	
    public boolean equals (PossibleAction action) {
        if (!(action instanceof BuyOrBidStartItem)) return false;
        BuyOrBidStartItem a = (BuyOrBidStartItem) action;
        return a.startItem == startItem
            && a.itemIndex == itemIndex
            && a.priceOrMinimumBid == priceOrMinimumBid;
    }

	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append ("BuyOrBidStartItem ").append(startItem.getName())
         .append (" status=").append(startItem.getStatusName());
        switch (getStatus()) {
        case StartItem.BIDDABLE:
			b.append(" bid=").append(actualBid);
            break;
        case StartItem.BUYABLE:
			b.append (" price=").append(startItem.getBasePrice());
            break;
        case StartItem.NEEDS_SHARE_PRICE:
			b.append(" startprice=").append(sharePrice);
            break;
		}
		return b.toString();
    }
	
	private void readObject (ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		
		startItem = StartItem.getByName (startItemName);
		
	}
}
