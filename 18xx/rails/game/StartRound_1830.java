/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StartRound_1830.java,v 1.13 2008/06/04 19:00:31 evos Exp $ */
package rails.game;

import java.util.*;

import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.StartItemAction;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.util.LocalText;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_1830 extends StartRound {
    int bidIncrement;

    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1830() {
        super();
        hasBidding = true;
    }

    /**
     * Start the 1830-style start round.
     * 
     * @param startPacket The startpacket to be sold in this start round.
     */
    public void start(StartPacket startPacket) {
        super.start(startPacket);
        bidIncrement = startPacket.getModulus();
        setPossibleActions();

    }

    public boolean setPossibleActions() {

        boolean passAllowed = true;

        possibleActions.clear();

        if (StartPacket.getStartPacket().areAllSold()) return false;

        StartItem auctionItem = (StartItem) auctionItemState.getObject();

        while (possibleActions.isEmpty()) {

            Player currentPlayer = getCurrentPlayer();

            for (StartItem item : itemsToSell) {

                if (item.isSold()) {
                    // Don't include
                } else if (item.getStatus() == StartItem.AUCTIONED) {
                    item.setStatus(StartItem.AUCTIONED);
                    if (currentPlayer.getFreeCash()
                        + auctionItem.getBid(currentPlayer) >= auctionItem.getMinimumBid()) {
                        BidStartItem possibleAction =
                                new BidStartItem(auctionItem,
                                        auctionItem.getMinimumBid(),
                                        startPacket.getModulus(), true);
                        possibleActions.add(possibleAction);
                        break; // No more actions
                    } else {
                        // Can't bid: Autopass
                        break;
                    }
                } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                    /* This status is set in buy() if a share price is missing */
                    possibleActions.add(new BuyStartItem(item, 0, false, true));
                    passAllowed = false;
                    break; // No more actions
                } else if (item == startPacket.getFirstUnsoldItem()) {
                    if (item.getBidders() == 1) {
                        // Bid upon by one player.
                        // If we need a share price, ask for it.
                        PublicCompanyI comp = item.needsPriceSetting();
                        if (comp != null) {
                            setPlayer(item.getBidder());
                            item.setStatus(StartItem.NEEDS_SHARE_PRICE);
                            BuyStartItem newItem =
                                    new BuyStartItem(item, item.getBasePrice(),
                                            true, true);
                            possibleActions.add(newItem);
                            break; // No more actions possible!
                        } else {
                            // Otherwise, buy it now.
                            assignItem(item.getBidder(), item, item.getBid(), 0);
                        }
                    } else if (item.getBidders() > 1) {
                        ReportBuffer.add(LocalText.getText("TO_AUCTION",
                                item.getName()));
                        // Start left of the currently highest bidder
                        if (item.getStatus() != StartItem.AUCTIONED) {
                            setNextBiddingPlayer(item,
                                    item.getBidder().getIndex());
                            currentPlayer = getCurrentPlayer();
                            item.setStatus(StartItem.AUCTIONED);
                            auctionItemState.set(item);
                            // numBidders = item.getBidders();
                        }
                        if (currentPlayer.getFreeCash()
                            + item.getBid(currentPlayer) >= item.getMinimumBid()) {
                            BidStartItem possibleAction =
                                    new BidStartItem(item,
                                            item.getMinimumBid(),
                                            startPacket.getModulus(), true);
                            possibleActions.add(possibleAction);
                        }
                        break; // No more possible actions!
                    } else {
                        item.setStatus(StartItem.BUYABLE);
                        if (currentPlayer.getFreeCash() >= item.getBasePrice()) {
                            possibleActions.add(new BuyStartItem(item,
                                    item.getBasePrice(), false));
                        }
                    }
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

            /*
             * it is possible that the last unsold item was sold in the above
             * loop. go to next round if that happened
             */
            if (StartPacket.getStartPacket().areAllSold()) {
                return false;
            }

            if (possibleActions.isEmpty()) {
                numPasses.add(1);
                if (auctionItemState.getObject() == null) {
                    setNextPlayer();
                } else {
                    setNextBiddingPlayer((StartItem) auctionItemState.getObject());
                }
            }
        }

        if (passAllowed) {
            possibleActions.add(new NullAction(NullAction.PASS));
        }

        return true;
    }

    /**
     * Return the start items, marked as appropriate for an 1830-style auction.
     */
    public List<StartItem> getStartItems() {

        return itemsToSell;
    }

    /*----- MoveSet methods -----*/
    /**
     * The current player bids on a given start item.
     * 
     * @param playerName The name of the current player (for checking purposes).
     * @param itemName The name of the start item on which the bid is placed.
     * @param amount The bid amount.
     */
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        int previousBid = 0;
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // Check player
            if (!playerName.equals(player.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName);
                break;
            }
            // Check item
            boolean validItem = false;
            for (StartItemAction activeItem : possibleActions.getType(StartItemAction.class)) {
                if (bidItem.equals(activeItem)) {
                    validItem = true;
                    break;
                }

            }
            if (!validItem) {
                errMsg =
                        LocalText.getText("ActionNotAllowed",
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
                errMsg =
                        LocalText.getText("BidTooLow", ""
                                                       + item.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidAmount % startPacket.getModulus() != 0) {
                errMsg =
                        LocalText.getText(
                                "BidMustBeMultipleOf",
                                new String[] {
                                        String.valueOf(bidAmount),
                                        String.valueOf(startPacket.getMinimumIncrement()) });
                break;
            }

            // Has the buyer enough cash?
            previousBid = item.getBid(player);
            int available = player.getFreeCash() + previousBid;
            if (bidAmount > available) {
                errMsg =
                        LocalText.getText("BidTooHigh", Bank.format(available));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidBid", new String[] {
                    playerName, item.getName(), errMsg }));
            return false;
        }

        MoveSet.start(false);

        item.setBid(bidAmount, player);
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(bidAmount);
        ReportBuffer.add(LocalText.getText("BID_ITEM_LOG", new String[] {
                playerName, Bank.format(bidAmount), item.getName(),
                Bank.format(player.getFreeCash()) }));

        if (bidItem.getStatus() != StartItem.AUCTIONED) {
            /* GameManager. */;
            setNextPlayer();
        } else {
            setNextBiddingPlayer(item);
        }
        numPasses.set(0);

        return true;

    }

    /**
     * Process a player's pass.
     * 
     * @param playerName The name of the current player (for checking purposes).
     */
    protected boolean pass(String playerName) {

        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        StartItem auctionItem = (StartItem) auctionItemState.getObject();

        while (true) {

            // Check player
            if (!playerName.equals(player.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidPass", new String[] {
                    playerName, errMsg }));
            return false;
        }

        ReportBuffer.add(LocalText.getText("PASSES", playerName));

        MoveSet.start(false);

        numPasses.add(1);

        if (auctionItem != null) {

            if (numPasses.intValue() == auctionItem.getBidders() - 1) {
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
            } else {
                // More than one left: find next bidder
                setNextBiddingPlayer(auctionItem,
                        GameManager.getCurrentPlayerIndex());
            }

        } else {

            if (numPasses.intValue() >= numPlayers) {
                // All players have passed.
                ReportBuffer.add(LocalText.getText("ALL_PASSED"));
                // It the first item has not been sold yet, reduce its price by
                // 5.
                if (startPacket.getFirstUnsoldItem() == startPacket.getFirstItem()) {
                    startPacket.getFirstItem().reduceBasePriceBy(5);
                    ReportBuffer.add(LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                            new String[] {
                                    startPacket.getFirstItem().getName(),
                                    Bank.format(startPacket.getFirstItem().getBasePrice()) }));
                    numPasses.set(0);
                    if (startPacket.getFirstItem().getBasePrice() == 0) {
                        assignItem(getCurrentPlayer(),
                                startPacket.getFirstItem(), 0, 0);
                        GameManager.setPriorityPlayer();
                        // startPacket.getFirstItem().getName());
                    }
                } else {
                    numPasses.set(0);
                    GameManager.getInstance().nextRound(this);

                }
            } else if (auctionItem != null) {
                setNextBiddingPlayer(auctionItem);
            } else {
                setNextPlayer();
            }
        }

        return true;
    }

    private void setNextBiddingPlayer(StartItem item, int currentIndex) {
        for (int i = currentIndex + 1; i < currentIndex
                                           + GameManager.getNumberOfPlayers(); i++) {
            if (item.hasBid(GameManager.getPlayer(i).getName())) {
                GameManager.setCurrentPlayerIndex(i);
                break;
            }
        }
    }

    private void setNextBiddingPlayer(StartItem item) {

        setNextBiddingPlayer(item, GameManager.getCurrentPlayerIndex());
    }

    public String getHelp() {
        return "1830 Start Round help text";
    }

}
