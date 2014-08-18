package net.sf.rails.game.specific._1862;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.StartItem;

public class StartItem_1862 extends StartItem {
    protected boolean[] actedOn; // Either bidded or passed
    protected boolean[] passed;
        
    protected StartItem_1862(RailsItem parent, String name, String type,
            int index, boolean president) {
        super(parent, name, type, index, president);
    }

    public static StartItem_1862 create(RailsItem parent, String name, String type, int price, int index, boolean president){
        StartItem_1862 item = new StartItem_1862(parent, name, type, index, president);
        item.initBasePrice(price);
        return item;
    }
    
    public void init(GameManager gameManager) {
        super.init(gameManager);
        passed = new boolean[numberOfPlayers];
        actedOn = new boolean[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            actedOn[i] = false;
            passed[i] = false;
        }
        // Override behavior in super class as far as minimum bid...
        minimumBid.set(basePrice.value());
    }
    
    public void setPass(Player player) {
        actedOn[player.getIndex()] = true;
        passed[player.getIndex()] = true;
    }

    public int getNumActivePlayers() {
        int n = 0;
        for (int i = 0; i < numberOfPlayers; i++) {
            if (passed[i] == false) {
                n++;
            }
        }
        return n;
    }
    
    public boolean isActivePlayer(Player player) {
        return (!passed[player.getIndex()]);
    }
    
    public void setBid(int amount, Player bidder) {
        int index = bidder.getIndex();
        actedOn[index] = true;
        bids[index].set(amount);
        bids[index].setSuppressZero(false);

        if (amount >= 0) {
            lastBidderIndex.set(index);
            minimumBid.set(amount + 5);
        } else if (amount == -1) {
            passed[index] = true;
            bids[index].set(0);
        }
    }

    
    
}
