package net.sf.rails.game.specific._1862;

import java.util.SortedSet;
import java.util.TreeSet;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.Certificate;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.StockMarket;
import net.sf.rails.game.StockSpace;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.OwnableItem;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_1862 extends StartRound {
    protected final int bidIncrement;
    
    private final GenericState<StartItem> auctionItemState = 
            GenericState.create(this, "auctionItemState");


    // TODO: Get correct ones when stock market is created

    private static SortedSet<StockSpace> startSpaces;
    static {        
        StockMarket stockMarket = new StockMarket(RailsRoot.getInstance(), "foo");

        startSpaces = new TreeSet<StockSpace>();
        startSpaces.add(StockSpace.create(stockMarket, "54", 54, null));
        startSpaces.add(StockSpace.create(stockMarket, "58", 58, null));
        startSpaces.add(StockSpace.create(stockMarket, "62", 62, null));
        startSpaces.add(StockSpace.create(stockMarket, "68", 68, null));
        startSpaces.add(StockSpace.create(stockMarket, "74", 74, null));
        startSpaces.add(StockSpace.create(stockMarket, "82", 82, null));
        startSpaces.add(StockSpace.create(stockMarket, "90", 90, null));
        startSpaces.add(StockSpace.create(stockMarket, "100", 100, null));
    }
    
    /**
     * Constructed via Configure
     */
    public StartRound_1862(GameManager parent, String id) {
        super(parent, id, true, false, false);
        bidIncrement = startPacket.getModulus();
    }
    
    @Override
    public void start() {
        super.start();
        auctionItemState.set(null);
        setPossibleActions();
    }

    @Override
    public boolean process(PossibleAction action) {
        if (!super.process(action)) return false;

        // Assign any further items that have been bid exactly once
        // and don't need any further player intervention, such
        // as setting a start price
//        StartItem item;
//        while ((item = startPacket.getFirstUnsoldItem()) != null
//                && item.getBidders() == 1 && item.needsPriceSetting() == null) {
//            assignItem(item.getBidder(), item, item.getBid(), 0);
//
//            // Check if this has exhausted the start packet
//            if (startPacket.areAllSold()) {
//                finishRound();
//                break;
//            }
//        }
        return true;
    }

    @Override
    public boolean setPossibleActions() {
        boolean passAllowed = true;

        possibleActions.clear();

        if (playerManager.getCurrentPlayer() == startPlayer) ReportBuffer.add(this, "");

        // FIXME: Rails 2.0 Could be an infinite loop if there if no player has enough money to buy an item
        while (possibleActions.isEmpty()) {
            Player currentPlayer = playerManager.getCurrentPlayer();

            boolean auctionStarted = false;
            for (StartItem item : itemsToSell.view()) {
                if (item.getStatus() == StartItem.AUCTIONED) {
                    auctionStarted = true;
                    if (currentPlayer.getFreeCash()
                            + item.getBid(currentPlayer) >= item.getMinimumBid()) {
                            BidStartItem possibleAction =
                                    new BidStartItem(item,
                                            item.getMinimumBid(),
                                            startPacket.getModulus(), true);
                            possibleActions.add(possibleAction);
                            break; // No more actions
                    } else {
                        // Can't bid: Autopass
                        numPasses.add(1);
                        break;
                    }
                }
            }
            
            if (auctionStarted == false) {
                for (StartItem item : itemsToSell.view()) {
                    if (item.isSold()) {
                        // Don't include
                    } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                        /* This status is set in buy() if a share price is missing */
                        playerManager.setCurrentPlayer(item.getBidder());
                        BuyStartItem bsi = new BuyStartItem(item, item.getBid(), false, true);
                        bsi.setStartSpaces(startSpaces);         
                        possibleActions.add(bsi);
                        passAllowed = false;
                        break; // No more actions
                    } else {
                    item.setStatus(StartItem.BIDDABLE);
                    if (currentPlayer.getFreeCash()
                        + item.getBid(currentPlayer) >= item.getMinimumBid()) {
                        BidStartItem possibleAction =
                                new BidStartItem(item, item.getMinimumBid(),
                                        startPacket.getModulus(), false);
                        possibleActions.add(possibleAction);
                    }
                }

            }
            }

            if (possibleActions.isEmpty()) {
                numPasses.add(1);
                if (auctionItemState.value() == null) {
                    playerManager.setCurrentToNextPlayer();
                } else {
                    setNextBiddingPlayer((StartItem_1862) auctionItemState.value());
                }
            }
        }

        if (passAllowed) {
            possibleActions.add(new NullAction(NullAction.Mode.PASS));
        }

       return true;
    }

    /*----- moveStack methods -----*/
    /**
     * The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param itemName The name of the start item on which the bid is placed.
     * @param amount The bid amount.
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int previousBid = 0;
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getId());
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
            if (bidItem.getStatus() != StartItem.BIDDABLE
                && bidItem.getStatus() != StartItem.AUCTIONED) {
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
            previousBid = item.getBid(player);
            int available = player.getFreeCash() + previousBid;
            if (bidAmount > available) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(this, available));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }

        

        item.setBid(bidAmount, player);
        item.setStatus(StartItem.AUCTIONED);
        auctionItemState.set(item);
        
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(this, bidAmount),
                item.getName(),
                Bank.format(this, player.getFreeCash()) ));

//        if (bidItem.getStatus() != StartItem.AUCTIONED) {
            playerManager.setCurrentToNextPlayer();
//        } else {
//            setNextBiddingPlayer(item);
//        }
        numPasses.set(0);

        return true;

    }
    
    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();
        int lastBid = item.getBid();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int price = 0;
        int sharePrice = 0;
        String shareCompName = "";


        while (true) {
            if (!boughtItem.setSharePriceOnly()) {
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

            if (boughtItem.hasSharePriceToSet()) {
                shareCompName = boughtItem.getCompanyToSetPriceFor();
                sharePrice = boughtItem.getAssociatedSharePrice();
                if (sharePrice == 0) {
                    errMsg =
                        LocalText.getText("NoSharePriceSet", shareCompName);
                    break;
                }
                // TODO: Add back in when stock market is created
//                if ((stockMarket.getStartSpace(sharePrice)) == null) {
//                    errMsg =
//                        LocalText.getText("InvalidStartPrice",
//                                    Bank.format(this, sharePrice),
//                                shareCompName );
//                    break;
//                }
            }
            break;
        }

        if (errMsg != null) {
            System.out.println(errMsg);
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }
        
        assignItem(player, item, price, sharePrice);

        // Set priority (only if the item was not auctioned)
        // ASSUMPTION: getting an item in auction mode never changes priority
        if (lastBid == 0) {
            playerManager.setPriorityPlayerToNext();
        }
        playerManager.setCurrentToNextPlayer();

        numPasses.set(0);
        auctionItemState.set(null);

        return true;
    }
    
    
    /**
     * Process a player's pass.
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        StartItem_1862 auctionItem = (StartItem_1862) auctionItemState.value();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getId());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidPass",
                    playerName,
                    errMsg ));
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

        numPasses.add(1);
        if (auctionItem != null) {

            if (numPasses.value() >= auctionItem.getNumActivePlayers() - 1) {
                // All but the highest bidder have passed.
                int price = auctionItem.getBid();

                log.debug("Highest bidder is "
                          + auctionItem.getBidder().getId());
                if (auctionItem.needsPriceSetting() != null) {
                    auctionItem.setStatus(StartItem.NEEDS_SHARE_PRICE);
                } else {
                    assignItem(auctionItem.getBidder(), auctionItem, price, 0);
                }
                auctionItemState.set(null);
                numPasses.set(0);
                // Next turn goes to priority holder
                playerManager.setCurrentToPriorityPlayer(); // TODO: Check
            } else {
                // More than one left: find next bidder

                if (GameOption.getAsBoolean(this, "LeaveAuctionOnPass")) {
                    // Game option: player to leave auction after a pass (default no).
                    player.unblockCash(auctionItem.getBid(player));
                    auctionItem.setPass(player);
//                    auctionItem.setBid(-1, player);  // TODO: Active
                }
                setNextBiddingPlayer(auctionItem);
            }

        } else {

            if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
                // All players have passed.
                ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
                // It the first item has not been sold yet, reduce its price by
                // 5.
                    numPasses.set(0);
                    //gameManager.nextRound(this);
                    finishRound();

//            } else if (auctionItem != null) {
                // TODO  Now dead code - should it be reactivated?
//                setNextBiddingPlayer(auctionItem);
            } else {
                playerManager.setCurrentToNextPlayer();
            }
        }

        return true;
    }

    
    protected void assignItem(Player player, StartItem item, int price,
            int sharePrice) {
        PublicCertificate primary = (PublicCertificate) item.getPrimary();
        primary.moveTo(player);
        String priceText = Currency.toBank(player, price);
        ReportBuffer.add(this,LocalText.getText("BuysItemFor",
                player.getId(),
                primary.getName(),
                priceText ));
        
        item.setSold(player, price);
    }

    
    private void setNextBiddingPlayer(StartItem_1862 item, Player biddingPlayer) {
        for (Player player:playerManager.getNextPlayersAfter(biddingPlayer, false, false)) {
            if (item.isActivePlayer(player)) {
                playerManager.setCurrentPlayer(player);
                break;
            }
        }
    }
    
    private void setNextBiddingPlayer(StartItem_1862 item) {
        setNextBiddingPlayer(item, playerManager.getCurrentPlayer());
    }

    @Override
    public String getHelp() {
        return "1862 Start Round help text";
    }

}