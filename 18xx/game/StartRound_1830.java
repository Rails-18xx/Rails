/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartRound_1830.java,v 1.6 2005/05/24 21:38:04 evos Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * Implements an 1830-style initial auction.
 * @author Erik Vos
 */
public class StartRound_1830 extends StartRound {
    
    
    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1830 () {
        super();
    }
    
    /**
     * Start the 1830-style start round.
     * @param startPacket The startpacket to be sold in this start round.
     */
    public void start (StartPacket startPacket) {
    	super.start (startPacket);
    }
    
    /**
     * Return the start round state (i.e. the next step to be executed).
     * @return A number prescribing the next player action. 
     */
    public int nextStep () {
        if (companyNeedingPrice != null) {
            return SET_PRICE;
        } else {
            return BID_OR_BUY;
        }
    }

    /**
     * Get a list of items that may be bought immediately.<p>
     * In an 1830-style auction this method will always return
     * only one item: the topmost unsold item.
     * @return An array of start items that can be bought.
     */
    public StartItem[] getBuyableItems () {
        if (startPacket.getItems().size() > 0) {
            return new StartItem[] {startPacket.getFirstUnsoldItem()};
        } else {
            return new StartItem[0];
        }
    }
    
    /**
     * Get a list of items that mat be bid upon.<p>
     * In an 1830-style auction this method will return an array
     * containing all unsold items except the topmost one. 
     * @return An array of start items that may be bid upon.
     */
    public StartItem[] getBiddableItems () {
        List bidItems = new ArrayList();
        Iterator it = startPacket.getItems().iterator();
        StartItem b;
        while (it.hasNext()) {
            if (!(b = (StartItem)it.next()).isSold() && b != startPacket.getFirstUnsoldItem()) {
                bidItems.add(b);
            }
        }
        return (StartItem[]) bidItems.toArray(new StartItem[0]);
    }
    
    /**
     * Get the currentPlayer.
     * @return The current Player object.
     * @see GameManager.getCurrentPlayer().
     */
    public Player getCurrentPlayer() {
        return GameManager.getCurrentPlayer();
    }
    
    /**
     * Get the company for which a par price must be set in 
     * the SET_PRICE state. In other states, null is returned.
     * @return The PublicCompany object for which a par price is needed.
     */
    public PublicCompanyI getCompanyNeedingPrice () {
        return companyNeedingPrice;
    }
    
    /*----- Action methods -----*/
    
    /**
     * The current player bids 5 more than the previous bid
     * on a given start item.<p>
     * A separate method is provided for this action because 5
     * is the usual amount with which bids are raised.
     * @param playerName The name of the current player (for checking purposes).
     * @param itemName The name of the start item on which the bid is placed.
     */
    public boolean bid5 (String playerName, String itemName) {
        
        // Only partial validation here
        StartItem item = (StartItem) itemMap.get(itemName);
        int amount = 0;
        if (item != null) amount = item.getMinimumBid();
        
        return bid (playerName, itemName, amount);

    }
    
    /**
     * The current player bids on a given start item. 
     * @param playerName The name of the current player (for checking purposes).
     * @param itemName The name of the start item on which the bid is placed.
     * @param amount The bid amount.
     */
    public boolean bid (String playerName, String itemName, int amount) {
        
        StartItem item = null;
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        int previousBid = 0;
        
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
            if (item == startPacket.getFirstUnsoldItem()) {
                errMsg = "Cannot bid on this item";
                break;
            }
            // Bid must be at least 5 above last bid
            if (amount < item.getMinimumBid()) {
                errMsg = "Bid too low, minimum is "+(item.getMinimumBid());
                break;
            }
            previousBid = item.getBid(player);
            int available = player.getUnblockedCash() + previousBid;
            if (amount > available) {
            	errMsg = "Bid too high, player has "+Bank.format(available)
					+" free for bidding";
            	break;
            }
            
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid bid by "+playerName+" on "+itemName+": " + errMsg);
            return false;
        }
        
        item.setBid (amount, player);
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(amount);
        Log.write (playerName+" bids "+Bank.format(amount)+" on "+itemName
        		+". Remains "+Bank.format(player.getUnblockedCash())+"");
        GameManager.setNextPlayer();
        numPasses = 0;
        
        return true;
        
    }
    
    /**
     * Define the next action to take after a start item is bought.
     *
     */
    protected void setNextAction() {
        
        if (companyNeedingPrice != null) {
            // Ask for the start price of a just obtained President's share
            // Nothing to do, current player keeps the turn.
            return;
        }
        
        if (startPacket.areAllSold()) {
            // No more start items: start a stock round
            GameManager.getInstance().nextRound(this);
            return;
        }
        
        StartItem nextItem;
        while ((nextItem = startPacket.getFirstUnsoldItem()) != null) {
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
    
    /**
     * Set a par price.
     * @param playerName The name of the par price setting player.
     * @param companyName The name of teh company for which a par price is set.
     * @param parPrice The par price.
     */
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
		        && Bank.getIpo().ownsShare(companyNeedingPrice) 
		        	<= (100 - companyNeedingPrice.getFloatPercentage())) {
			// Float company 
			companyNeedingPrice.setFloated();
			Log.write (companyName+ " floats and receives "
			        +Bank.format(companyNeedingPrice.getCash()));
		}
        
        companyNeedingPrice = null;
        
        setNextAction();
        
        return true;
    }
    
    /**
     * Process a player's pass.
     * @param playerName The name of the current player (for checking purposes).
     */
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
            // All players have passed. 
            Log.write("All players passed");
            // It the first item has not been sold yet, reduce its price by 5.
            if (startPacket.getFirstUnsoldItem() == startPacket.getFirstItem()) {
                startPacket.getFirstItem().basePrice -= 5;
                Log.write("Price of "+startPacket.getFirstItem().getName()+" now reduced to "
                        +Bank.format(startPacket.getFirstItem().basePrice));
                numPasses = 0;
                if (startPacket.getFirstItem().basePrice == 0) {
                    // If price drops to zero, the first player must buy the first private.
                    buy (getCurrentPlayer().getName(),
                            startPacket.getFirstItem().getName());
                }
            } else {
                // Otherwise, end of start round
                 GameManager.getInstance().nextRound(this);
            }
        }
        
        return true;
    }
    
    /*----- Internal functions -----*/
    protected boolean isBuyable (StartItem item) {
    	return !item.isSold() 
				&& item == startPacket.getFirstUnsoldItem();
    }
    
    protected boolean isBiddable (StartItem item) {
    	return !item.isSold()
				&& item != startPacket.getFirstUnsoldItem();
    }

}
