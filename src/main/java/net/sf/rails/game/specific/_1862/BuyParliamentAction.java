package net.sf.rails.game.specific._1862;

import net.sf.rails.game.Player;
import rails.game.action.PossibleAction;

public class BuyParliamentAction extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private ParliamentBiddableItem biddable;
    private int parPrice;
    private int sharesPurchased;
    private Player buyer;
    
    public BuyParliamentAction(ParliamentBiddableItem biddable, Player buyer) {
        this.biddable = biddable;
        this.parPrice = 0;
        this.sharesPurchased = 0;
        this.buyer = buyer;    
    }
    
    public void setParPrice(int parPrice) {
        this.parPrice = parPrice;
    }
    
    public void setSharesPurchased(int sharesPurchased) {
        this.sharesPurchased = sharesPurchased;
    }
    
    public ParliamentBiddableItem getBiddable() {
        return biddable;
    }
    
    public int getParPrice() {
        return parPrice;
    }
    
    public int getSharesPurchased() {
        return sharesPurchased;
    }
    
    public Player getBuyer() {
        return buyer;
    }    
}
