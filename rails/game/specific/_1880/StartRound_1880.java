/**
 * 
 */
package rails.game.specific._1880;

import rails.game.*;
import rails.game.action.*;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.game.state.ArrayListState;
import rails.util.LocalText;



/**
 * @author Martin
 *
 */
public class StartRound_1880 extends StartRound {
   
    private final State startingPlayer =
        new State("StartingPlayer", Player.class);
    
    private final IntegerState currentBuyPrice =
        new IntegerState("CurrentBuyPrice", 0);
    
    private final IntegerState initialItemRound = 
        new IntegerState("InitialItemRound",0); 
    
    private final State currentItem = 
        new State("CurrentItem", StartItem.class);
    
    private final IntegerState currentStartRoundPhase = 
        new IntegerState("CurrentStartRoundPhase",0); 
    
   private final IntegerState investorChosen = 
        new IntegerState("InvestorChosen",0);
    
    /** A company in need for a par price. */
    PublicCompanyI companyNeedingPrice = null;
    
    
    /**
     * @param gameManager
     */
    public StartRound_1880(GameManagerI gameManager) {
        super(gameManager);
        hasBasePrices=true;
        hasBidding=true;

    }
    @Override
    public void start() {
        super.start();
        
        // crude fix for StartItem hardcoded SetMinimumbid ignoring the initial value out of the XMLs....
        for (StartItem item : startPacket.getItems()) {
            item.setMinimumBid(item.getBasePrice());
        }
        startingPlayer.set(getCurrentPlayer());
        setPossibleActions();
        
    }

    @Override
    public boolean setPossibleActions() {
          
        possibleActions.clear();
        
        StartItem item = startPacket.getFirstUnsoldItem();
       
        
        //Need Logic to check for all Type Minor/Investor Certificate
        if ( (item.getType()!=null ) && (item.getType().equals("Private"))) {
            
            currentBuyPrice.set(item.getMinimumBid());
            
            if (currentPlayer == startPlayer) ReportBuffer.add("");
            
            if (currentItem == null || currentItem.get() != item ) { // we haven't seen this item before
                numPasses.set(0); // new round so cancel all previous passes !
                currentItem.set(item);
                item.setStatus(StartItem.BIDDABLE);
                item.setStatus(StartItem.BUYABLE);
                auctionItemState.set(item);
                initialItemRound.set(0);
            } else {
                initialItemRound.add(1);
            }
            
             
          
                Player currentPlayer = getCurrentPlayer();
                
                if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {  //still necessary ??
                    /* This status is set in buy() if a share price is missing */
                    setPlayer(item.getBidder());
                    possibleActions.add(new BuyStartItem(item, item.getBid(), false, true));
                    return true;
                    // No more actions
                }
                if ((item.getBidder() == currentPlayer) && (numPasses.intValue() == getNumberOfPlayers()-1)){ // Current Player is highest Bidder & all others have passed
                    if (item.needsPriceSetting() != null ){
                        BuyStartItem possibleAction = new BuyStartItem(item,item.getBid(), true, true);
                        possibleActions.add(possibleAction);
                       return true;
                         // No more actions// no further Actions possible
                    }else{
                        BuyStartItem possibleAction = new BuyStartItem(item,item.getBid(),true);
                        possibleActions.add(possibleAction);
                        return true;
                         // No more actions// no further Actions possible
                    }
                }
                
                if (currentPlayer.getCash() >= item.getMinimumBid()) {
                    //Kann bieten
                    if (item.getBid() == 0) { // erster Spieler noch keiner sonst geboten.
                    BidStartItem possibleAction =
                        new BidStartItem(item, item.getBasePrice(),
                            0, true);
                    possibleActions.add(possibleAction); // Player can offer a bid
                    possibleActions.add(new NullAction(NullAction.PASS));
                    return true;
                    } else {
                        BidStartItem possibleAction =
                            new BidStartItem(item, item.getMinimumBid(),
                                startPacket.getModulus(), true);
                        possibleActions.add(possibleAction); // Player can offer a bid
                        possibleActions.add(new NullAction(NullAction.PASS));
                        return true;
                    }
                } else {
                    // Can't bid: Autopass
                    numPasses.add(1);
                    return false;
                }
        } else { // Item is not a private ! should be a major or minor in 1880 special rules apply.
            //Check if all players own a minor/investor already then declare Startinground over...
            if (currentStartRoundPhase.intValue() == 0) { //first time a non Private gets called up; initialize the rest of items to BUYABLE
                      // Priority Deal goes to the player with the smallest wallet...
                     gameManager.setCurrentPlayer(gameManager.reorderPlayersByCash(true));
                     //setCurrentPlayerIndex(0); //lowest or highest Player is always at the start of the player list after reordering !
                     //Send Message that Playerorder has Changed !...
                     currentPlayer=getCurrentPlayer();
                     currentStartRoundPhase.set(1);
                     startingPlayer.set(currentPlayer);
                     gameManager.setPriorityPlayer((Player) startingPlayer.get()); // Method doesn't exist in Startround ???
            }
           if (investorChosen.intValue() == getNumberOfPlayers()) {
               for ( StartItem item1 : itemsToSell) {
                   if (!item1.isSold()){
                       item1.setStatus(StartItem.UNAVAILABLE);
                       item1.setStatus(StartItem.SOLD);
                      
                   }
               }
               finishRound();
               return false;
           } else {
               for ( StartItem item1 : itemsToSell) {
                       if (!item1.isSold()){
                           item1.setStatus(StartItem.BUYABLE);
                           BuyStartItem possibleAction = new BuyStartItem(item1, 0, false);
                           possibleActions.add(possibleAction);
                       }
                   }   
                investorChosen.add(1);
                return true;
              }
               
       }
    }
   
    /* (non-Javadoc)
     * @see rails.game.StartRound#bid(java.lang.String, rails.game.action.BidStartItem)
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {
        StartItem item = bidItem.getStartItem();
        String errMsg = null;
        Player player = getCurrentPlayer();
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // Check player
            if (!playerName.equals(player.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getName());
                break;
            }
            // Check item
            boolean validItem = false;
            for (StartItemAction activeItem : possibleActions.getType(StartItemAction.class)) {
                if (bidItem.equalsAsOption(activeItem)) {
                    validItem = true;
                    break;
                }

            }
            if (!validItem) {
                errMsg = LocalText.getText("ActionNotAllowed",
                                bidItem.toString());
                break;
            }

            // Is the item buyable?
            if (bidItem.getStatus() != StartItem.BUYABLE) {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            // Bid must be at least 5 above last bid
            if (bidAmount < item.getMinimumBid()) {
                errMsg = LocalText.getText("BidTooLow", ""
                                                       + item.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidAmount % startPacket.getModulus() != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf",
                                bidAmount,
                                startPacket.getMinimumIncrement());
                break;
            }

            // Has the buyer enough cash?
            if (bidAmount > player.getCash()) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(bidAmount));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidBid",
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }

        moveStack.start(false);

        item.setBid(bidAmount, player);
        ReportBuffer.add(LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(bidAmount),
                item.getName(),
                Bank.format(player.getCash()) ));
         setNextBiddingPlayer(item);
         return true;

    }

    /* (non-Javadoc)
     * @see rails.game.StartRound#pass(java.lang.String)
     */
    @Override
    protected boolean pass(String playerName) {
        String errMsg = null;
        Player player = getCurrentPlayer();
        StartItem auctionItem = (StartItem) auctionItemState.get();

        while (true) {

            // Check player
            if (!playerName.equals(player.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getName());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidPass",
                    playerName,
                    errMsg ));
            return false;
        }

        ReportBuffer.add(LocalText.getText("PASSES", playerName));

        moveStack.start(false);

        numPasses.add(1);
        
        if (numPasses.intValue() >= numPlayers) {
            // All players have passed.
            ReportBuffer.add(LocalText.getText("ALL_PASSED"));
            // It the first item has not been sold yet, reduce its price by
            // 5.
            if (auctionItem.getIndex() < 2) {
                auctionItem.reduceBasePriceBy(5);
                auctionItem.setMinimumBid(auctionItem.getBasePrice());
                ReportBuffer.add(LocalText.getText(
                        "ITEM_PRICE_REDUCED",
                                auctionItem.getName(),
                                Bank.format(startPacket.getFirstItem().getBasePrice()) ));
                numPasses.set(0);
                if (auctionItem.getBasePrice() == 0) {
                    assignItem((Player)startingPlayer.get(),
                            auctionItem, 0, 0);
                    setNextStartingPlayer();
                    // startPacket.getFirstItem().getName());
                    return true;
                }
            } else {
                numPasses.set(0);
                //gameManager.nextRound(this);
                finishRound();

            }
        }
       // if ((numPasses.intValue() >= auctionItem.getBidders() - 1) && 
        if ((auctionItem.getBidders() >0) && (numPasses.intValue()== getNumberOfPlayers()-1)) {
            // All but the highest bidder have passed.
            int price = auctionItem.getBid();

            log.debug("Highest bidder is "
                      + auctionItem.getBidder().getName());
            if (auctionItem.needsPriceSetting() != null) {
                auctionItem.setStatus(StartItem.NEEDS_SHARE_PRICE);
            } else {
                assignItem(auctionItem.getBidder(), auctionItem, price, 0);
            }
            auctionItemState.set(null);
            numPasses.set(0);
           setNextStartingPlayer();
           return true;
        } else {
            // More than one left: find next bidder

            if (auctionItem.getIndex()>1){
                auctionItem.setBid(-1, player);
                setNextBiddingPlayer(auctionItem,
                        getCurrentPlayerIndex());          
            }else {
                auctionItem.setBid(-1, player);
                setNextPlayer();
            }
           
           
        }
           
         
             
    return true;
   

    }
    
    private void setNextBiddingPlayer(StartItem item, int currentIndex) {
        for (int i = currentIndex + 1; i < currentIndex
                                           + gameManager.getNumberOfPlayers(); i++) {
            if (item.getBid(gameManager.getPlayerByIndex(i)) >=0) {
                setCurrentPlayerIndex(i);
                break;
            }
        }
    }

    private void setNextBiddingPlayer(StartItem item) {

        setNextBiddingPlayer(item, getCurrentPlayerIndex());
    }

    @Override
    public String getHelp() {
        return "1880 Start Round help text";
    }
    
    private void setNextStartingPlayer(){
        int i;
        Player player;
        player = (Player) startingPlayer.get();
        i= player.getIndex();
        startingPlayer.set(gameManager.getPlayerByIndex(i+1));
        setCurrentPlayerIndex(i+1 % getNumberOfPlayers());
    }

}
