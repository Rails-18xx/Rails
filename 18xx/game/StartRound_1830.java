/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartRound_1830.java,v 1.2 2005/05/13 22:13:28 evos Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * @author Erik Vos
 */
public class StartRound_1830 extends StartRound {
    
    public static final int BID_OR_BUY = 0;
    public static final int SET_PRICE = 1;
    
    PublicCompanyI companyNeedingPrice = null;   
    
    /**
     * Will be created dynamically.
     *
     */
    public StartRound_1830 () {
        super();
    }
    
    public void start (StartPacket startPacket) {
        
        this.startPacket = startPacket;
        itemMap = new HashMap ();
        Iterator it = startPacket.getItems().iterator();
        StartItem item;
        while (it.hasNext()) {
            item = (StartItem) it.next();
            itemMap.put(item.getName(), item);
        }
        
        GameManager.getInstance().setRound(this);
        GameManager.setCurrentPlayerIndex (GameManager.getPriorityPlayerIndex());
        Log.write (getCurrentPlayer().getName() + " has the Priority Deal");
    }
    
    public int nextStep () {
        if (companyNeedingPrice != null) {
            return SET_PRICE;
        } else {
            return BID_OR_BUY;
        }
    }

    public StartItem[] getBuyableItems () {
        if (startPacket.getItems().size() > 0) {
            return (StartItem[]) startPacket.getItems().subList(0, 1).toArray(new StartItem[0]);
        } else {
            return new StartItem[0];
        }
    }
    
    public StartItem[] getBiddableItems () {
        int length;
        if ((length = startPacket.getItems().size()) > 1) {
            return (StartItem[]) startPacket.getItems().subList(1, length).toArray(new StartItem[0]);
        } else {
            return new StartItem[0];
        }
    }
    
    public Player getCurrentPlayer() {
        return GameManager.getCurrentPlayer();
    }
    
    public PublicCompanyI getCompanyNeedingPrice () {
        return companyNeedingPrice;
    }
    
    /*----- Action methods -----*/
    
    public boolean bid5 (String playerName, String itemName) {
        
        // Only partial validation here
        StartItem item = (StartItem) itemMap.get(itemName);
        int amount = 0;
        if (item != null) amount = item.getMinimumBid();
        
        return bid (playerName, itemName, amount);

    }
    
    public boolean bid (String playerName, String itemName, int amount) {
        
        StartItem item = null;
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        
        while (true) {
            
            // Check player
            if (!playerName.equals(player.getName())) {
               errMsg = "Wrong player";
               break;
            }
            // Check name of item
            if (!itemMap.containsKey(itemName)) {
                errMsg = "Not found";
                break;
            }
            item = (StartItem) itemMap.get(itemName);
            // Must not be the first item
            if (item == startPacket.getItems().get(0)) {
                errMsg = "Cannot bid on this item";
                break;
            }
            // Bid must be at least 5 above last bid
            if (amount < item.getMinimumBid()) {
                errMsg = "Bid too low, minimum is "+(item.getMinimumBid());
                break;
            }
            /** TODO Player must have enough uncommitted money. */
            
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid bid by "+playerName+" on "+itemName+": " + errMsg);
            return false;
        }
        
        Log.write (playerName+" bids "+amount+" on "+itemName);
        item.setBid (amount, player);
        GameManager.setNextPlayer();
        numPasses = 0;
        
        return true;
        
    }
    
    /** 
     * Buy a start item against the base price.
     * @param playerName Name of the buying player.
     * @param itemName Name of the bought start item.
     * @return False in case of any errors.
     */
    public boolean buy (String playerName, String itemName) {
        StartItem item = null;
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        
        while (true) {
            
            // Check player
             if (!playerName.equals(player.getName())) {
                errMsg = "Wrong player";
                break;
            }
            // Check name of item
            if (!itemMap.containsKey(itemName)) {
                errMsg = "Not found";
                break;
            }
            item = (StartItem) itemMap.get(itemName);
            // Must  be the first item
            if (item != startPacket.getItems().get(0)) {
                errMsg = "Cannot buy this item";
                break;
            }
            
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid buy by "+playerName+" of "+itemName+": " + errMsg);
            return false;
        }
        
        assignItem (player, item, item.getBasePrice());
        
        // Set priority
        GameManager.setPriorityPlayerIndex(GameManager.getCurrentPlayerIndex() + 1);
        numPasses = 0;
        
        // Next action
        setNextAction();        
        return true;
            
    }
    
    private void assignItem (Player player, StartItem item, int price) {
        
        Log.write (player.getName()+" buys "+item.getName()+" for "+Bank.format(price));
        Certificate primary = item.getPrimary();
        player.buy(primary, price);
        if (primary instanceof PublicCertificateI 
                && ((PublicCertificateI)primary).isPresidentShare()) {
            // We must set the start price!
            companyNeedingPrice = ((PublicCertificateI)primary).getCompany();
        }
       if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            player.buy (extra, 0);
            Log.write (player.getName()+" also gets "+extra.getName());
            if (extra instanceof PublicCertificateI 
                    && ((PublicCertificateI)extra).isPresidentShare()) {
                // We must set the start price!
                companyNeedingPrice = ((PublicCertificateI)extra).getCompany();
            }
        }
        startPacket.getItems().remove(0);
    }
    
    /**
     * Define the next action to take after a start item is bought.
     *
     */
    private void setNextAction() {
        
        if (companyNeedingPrice != null) {
            // Ask for the start price of a just obtained President's share
            // Nothing to do, current player keeps the turn.
            return;
        }
        
        if (startPacket.getItems().isEmpty()) {
            // No more start items: start a stock round
            GameManager.getInstance().nextRound(this);
            return;
        }
        
        StartItem nextItem;
        while ((nextItem = startPacket.getFirstItem()) != null) {
	        if (nextItem.getBids() == 1) {
	            // Assign next item to the only bidder
	            assignItem (nextItem.getBidder(), nextItem, nextItem.getBid());
	        } else if (nextItem.getBids() > 1) {
	            // More than one bid on the next item: start a bid round.
	            // Pending that, assign to the highest bidder.
	            assignItem (nextItem.getBidder(), nextItem, nextItem.getBid());
	         } else {
	             // Next item has no bids yet
	             GameManager.setCurrentPlayer(GameManager.getPriorityPlayer());
	             break;
	         }
        }
    }
    
    public boolean setPrice (String playerName, String companyName, int parPrice) {
        
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        StockSpaceI startSpace = null;
        
        while (true) {
            
            // Check player
             if (!playerName.equals(player.getName())) {
                errMsg = "Wrong player";
                break;
            }
            // Check company
            if (!companyName.equals(companyNeedingPrice.getName())) {
                errMsg = "Wrong company";
                break;
            }
            // Check par price
            if ((startSpace = StockMarket.getInstance().getStartSpace(parPrice)) == null) {
                errMsg = "Invalid par price";
                break;
            }
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid par price "+Bank.format(parPrice)+" set by "+playerName
                    +" for" + companyName+": " + errMsg);
            return false;
        }
        
        Log.write (playerName+" starts "+companyName+" at "+Bank.format(parPrice));
        companyNeedingPrice.start(startSpace);
        
        // Check if company already floats
        // Check if the company has floated
        /* Shortcut: float level and capitalisation hardcoded */
		if (!companyNeedingPrice.hasFloated() 
		        && Bank.getIpo().countShares(companyNeedingPrice) 
		        	<= (100 - companyNeedingPrice.getFloatPercentage())) {
			// Float company (limit and capitalisation to be made configurable)
			companyNeedingPrice.setFloated(10*parPrice);
			Log.write (companyName+ " floats and receives "
			        +Bank.format(companyNeedingPrice.getCash()));
		}
        
        companyNeedingPrice = null;
        
        setNextAction();
        
        return true;
    }
    
    public boolean pass (String playerName) {

        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        
        while (true) {
            
            // Check player
             if (!playerName.equals(player.getName())) {
                errMsg = "Wrong player";
                break;
            }
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid pass by "+playerName+": " + errMsg);
            return false;
        }
        
        Log.write (playerName+" passes.");
        GameManager.setNextPlayer();
        
        if (++numPasses >= numPlayers) {
            // End of start round
            Log.write("All players passed");
            GameManager.getInstance().nextRound(this);
        }
        
        return true;
    }
    

}
