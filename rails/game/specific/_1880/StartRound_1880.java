/**
 * 
 */
package rails.game.specific._1880;

import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.state.IntegerState;
import rails.game.state.State;

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
   
   protected IntegerState playersActed = new IntegerState("PlayersActedinStartround");
    
    /** A company in need for a par price. */
    PublicCompanyI companyNeedingPrice = null;
    
    private final List<Player> playersPassed = new Vector<Player>();
    
    
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
                playersActed.set(0);
                playersPassed.clear();
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
                    possibleActions.add(new BuyStartItem_1880(item, item.getBid(), false, true));
                    return true;
                    // No more actions
                }
                if ((item.getBidder() == currentPlayer) && (numPasses.intValue() == getNumberOfPlayers()-1)){ // Current Player is highest Bidder & all others have passed
                    if (item.needsPriceSetting() != null ){
                        BuyStartItem_1880 possibleAction = new BuyStartItem_1880(item,item.getBid(), true, true);
                        possibleActions.add(possibleAction);
                       return true;
                         // No more actions// no further Actions possible
                    }else{
                        BuyStartItem_1880 possibleAction = new BuyStartItem_1880(item,item.getBid(),true);
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
                    playersActed.add(1);
                    playersPassed.add(currentPlayer);
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
           
            for ( StartItem item1 : itemsToSell) {
                       if (!item1.isSold()){
                           item1.setStatus(StartItem.BUYABLE);
                           BuyStartItem_1880 possibleAction = new BuyStartItem_1880(item1, 0, false);
                           possibleActions.add(possibleAction);
                       }
                   }   
                investorChosen.add(1);
                return true;
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

        if (! item.hasBid(player)) {
        playersActed.add(1);
        }
        item.setBid(bidAmount, player);
        ReportBuffer.add(LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(bidAmount),
                item.getName(),
                Bank.format(player.getCash()) ));

        if ((item.getBidders() >0) && (numPasses.intValue()== getNumberOfPlayers()-1)) {
            // All but the highest bidder have passed.
            int price = item.getBid();

            log.debug("Highest bidder is "
                      + item.getBidder().getName());
            if (item.needsPriceSetting() != null) {
                item.setStatus(StartItem.NEEDS_SHARE_PRICE);
            } else {
                assignItem(item.getBidder(), item, price, 0);
            }
            auctionItemState.set(null);
            numPasses.set(0);
            playersActed.set(0);
            playersPassed.clear();
           setNextStartingPlayer();
           return true;
        } else {
         setNextBiddingPlayer(item);
         return true;
        }

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
        playersActed.add(1);
        playersPassed.add(player);
        
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
                playersActed.set(0);
                playersPassed.clear();
                if (auctionItem.getBasePrice() == 0) {
                    assignItem((Player)startingPlayer.get(),
                            auctionItem, 0, 0);
                    setNextStartingPlayer();
                    return true;
                }
            } else {
                numPasses.set(0);
                playersActed.set(0);
                playersPassed.clear();
                finishRound();

            }
        }

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
            playersActed.set(0);
            playersPassed.clear();
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
            if (playersPassed.contains(gameManager.getPlayerByIndex(i)) == false) {
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
    /* (non-Javadoc)
     * @see rails.game.StartRound#startPacketChecks()
     */
    @Override
    protected void startPacketChecks() {
        // TODO Auto-generated method stub
        super.startPacketChecks();
        if (investorChosen.intValue() == getNumberOfPlayers()) {
            for ( StartItem item1 : itemsToSell) {
                if (!item1.isSold()){
                    item1.setStatus(StartItem.UNAVAILABLE);
                    item1.setStatus(StartItem.SOLD);
                   
                }
            }
        }

    }
    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1880. 
     */
    @Override
    protected void floatCompany(PublicCompanyI company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
     
        cash = 5 * price;
             
        company.setFloated(); 

        if (cash > 0) {
            new CashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getName(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

    }
    /* (non-Javadoc)
     * @see rails.game.StartRound#buy(java.lang.String, rails.game.action.BuyStartItem)
     * 
     * Buy a start item against the base price.
     *
     * @param playerName Name of the buying player.
     * @param itemName Name of the bought start item.
     * @param sharePrice If nonzero: share price if item contains a President's
     * share
     * @return False in case of any errors.
     */
     
    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
            StartItem item = boughtItem.getStartItem();
            int lastBid = item.getBid();
            String errMsg = null;
            Player player = getCurrentPlayer();
            int price = 0;
            int sharePrice = 0;
            String shareCompName = "";
            BitSet buildingRights = new BitSet(5);
            BuyStartItem_1880 boughtItem_1880 = (BuyStartItem_1880) boughtItem;

            while (true) {
                if (!boughtItem_1880.setSharePriceOnly()) {
                    if (item.getStatus() != StartItem.BUYABLE) {
                        errMsg = LocalText.getText("NotForSale");
                        break;
                    }

                    price = item.getBasePrice();
                    if (item.getBid() > price) price = item.getBid();

                    if (player.getFreeCash() < price) {
                        errMsg = LocalText.getText("NoMoney");
                        break;
                    }
                } else {
                    price = item.getBid();
                }

                if (boughtItem_1880.hasSharePriceToSet()) {
                    shareCompName = boughtItem_1880.getCompanyToSetPriceFor();
                    sharePrice = boughtItem_1880.getAssociatedSharePrice();
                    buildingRights = boughtItem_1880.getAssociatedBuildingRight();
                    
                    if (sharePrice == 0) {
                        errMsg =
                                LocalText.getText("NoSharePriceSet", shareCompName);
                        break;
                    }
                    
                    if (buildingRights.isEmpty()) {
                        errMsg = 
                                LocalText.getText("NoBuildingRightSet", shareCompName);
                        break;
                    }
                    if ((stockMarket.getStartSpace(sharePrice)) == null) {
                        errMsg =
                                LocalText.getText("InvalidStartPrice",
                                        Bank.format(sharePrice),
                                        shareCompName );
                        break;
                    }
                }
                break;
            }

            if (errMsg != null) {
                DisplayBuffer.add(LocalText.getText("CantBuyItem",
                        playerName,
                        item.getName(),
                        errMsg ));
                return false;
            }

            moveStack.start(false);

            assignItem(player, item, price, sharePrice, buildingRights);

            // Set priority (only if the item was not auctioned)
            // ASSUMPTION: getting an item in auction mode never changes priority
            if (lastBid == 0) {
                gameManager.setPriorityPlayer();
            }

            // If this item is the "IG" (BCR), then we are still in the "auction" portion
            // of the stock round, and the next player is based on the current player
            // (not the current player).
            if (item == StartItem.getByName("IG")) {
                setCurrentPlayerIndex(((Player) startingPlayer.get()).getIndex());
            } else {
                setNextPlayer();
            }

            auctionItemState.set(null);
            numPasses.set(0);

            return true;

        }
    /**
     * @param player
     * @param item
     * @param price
     * @param sharePrice
     * @param buildingRights
     */
    private void assignItem(Player player, StartItem item, int price,
            int sharePrice, BitSet buildingRights) {
        Certificate primary = item.getPrimary();
        ReportBuffer.add(LocalText.getText("BuysItemFor",
                player.getName(),
                primary.getName(),
                Bank.format(price) ));
        pay (player, bank, price);
        transferCertificate (primary, player.getPortfolio());
        checksOnBuying(primary, sharePrice, buildingRights);
        if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            ReportBuffer.add(LocalText.getText("ALSO_GETS",
                    player.getName(),
                    extra.getName() ));
            transferCertificate (extra, player.getPortfolio());
            checksOnBuying(extra, sharePrice, buildingRights);
        }
        item.setSold(player, price);
        
    }
    /**
     * @param cert
     * @param sharePrice
     * @param buildingRights
     */
    private void checksOnBuying(Certificate cert, int sharePrice,
            BitSet buildingRights) {
        if (cert instanceof PublicCertificateI) {
            PublicCertificateI pubCert = (PublicCertificateI) cert;
            PublicCompany_1880 comp = (PublicCompany_1880) pubCert.getCompany();
            // Start the company, look for a fixed start price
            if (!comp.hasStarted()) {
                if (!comp.hasStockPrice()) {
                    comp.start();
                } else if (pubCert.isPresidentShare()) {
                    /* Company to be started. Check if it has a start price */
                    if (sharePrice > 0) {
                        // User has told us the start price
                        comp.start(sharePrice);
                        //Building Rights are also set..
                        comp.setBuildingRights(buildingRights);
                        comp.setRight("BuildingRight",buildingRightToString(buildingRights));
                    } else {
                        log.error("No start price for " + comp.getName());
                    }
                }
            }
            if (comp.hasStarted() && !comp.hasFloated()) {
                checkFlotation(comp);
            }
            PublicCompany_1880 compX=(PublicCompany_1880) companyManager.getPublicCompany("BCR");
            if ((comp.getTypeName().equals("Minor")) && (comp.getPresident().getPortfolio().findCertificate(compX, true)!=null)) {
                //
                PublicCertificateI cert2;
                cert2 = ipo.findCertificate(compX, 1, false);
                if (cert2 == null) {
                        log.error("Cannot find " + compX.getName() + " " + 1*10
                                + "% share in " + ipo.getName());
                    }
                    cert2.moveTo(comp.getPortfolio());
                    comp.setDestinationHex(compX.getHomeHexes().get(0));
                    comp.setInfoText(comp.getInfoText() + "<br>Destination: "+comp.getDestinationHex().getInfo());
            }
        }
        
    }
    public String buildingRightToString (BitSet buildingRight){
        String buildingRightString = null;
        
      if (! buildingRight.isEmpty()){
         if (buildingRight.get(0)== true) {
              buildingRightString = "A";
               if (buildingRight.get(1) == true) {
                    buildingRightString = "A+B";
                    if (buildingRight.get(2) == true) {
                        buildingRightString = "A+B+C";
                    }
                }
            }
            else if (buildingRight.get(1) == true) {
                    buildingRightString = "B";
                    if (buildingRight.get(2) == true) {
                        buildingRightString = "B+C";
                      if (buildingRight.get(3) == true){
                           buildingRightString = "B+C+D";
                      }
                   }
            }
           else if (buildingRight.get(2) == true){
              buildingRightString = "C";
                if (buildingRight.get(3) == true){
                    buildingRightString = "C+D";
                }
            }
            else if (buildingRight.get(3) == true){
               buildingRightString= "D";
           }
       return buildingRightString;
       }
       return "None";
   }
    
}
