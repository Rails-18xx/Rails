/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyOrBidStartItem.java,v 1.2 2007/07/05 17:57:54 evos Exp $
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
public class BuyOrBidStartItem extends PossibleAction {
    
	/* Server-provided fields */
    private StartItem startItem;
    private boolean sharePriceToSet = false;
    private String companyNeedingSharePrice = null;
    
    /** Status of the start item (buyable? biddable?) for the
     * <i>current</i> player, taking into account the amount of
     * cash of this player that is blocked by bids on other items.
     */
    private int status;
    private int priceOrMinimumBid;
    private int itemIndex;
    
    /* Client-provided fields */
    private int actualBid = 0;
    private int sharePrice = 0;
    
    /**
     * 
     */
    public BuyOrBidStartItem(StartItem startItem, 
    		int priceOrMinimumBid, int status) {
    	super();
        this.startItem = startItem;
        this.itemIndex = startItem.getIndex();
        this.priceOrMinimumBid = priceOrMinimumBid;
        this.status = status;
        
        PublicCompanyI company;
        if ((company = startItem.needsPriceSetting()) != null) {
        	sharePriceToSet = true;
        	companyNeedingSharePrice = company.getName();
        }
        log.debug("For "+startItem.getName()+": companyNeedingSharePrice set to "+companyNeedingSharePrice);
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
		return status;
	}
	
	public boolean equals (BuyOrBidStartItem otherItem) {
		return startItem.getName().equals(otherItem.getStartItem().getName()) 
			&& status == otherItem.status;
	}
	
    public boolean equals (PossibleAction action) {
        if (!(action instanceof BuyOrBidStartItem)) return false;
        BuyOrBidStartItem a = (BuyOrBidStartItem) action;
        return a.startItem == startItem
            && a.itemIndex == itemIndex
            && a.priceOrMinimumBid == priceOrMinimumBid
            && a.status == status;
    }

	public String toString() {
        return "BuyOrBidStartItem "+ startItem.getName()
        	+ " status="+status;
    }
}
