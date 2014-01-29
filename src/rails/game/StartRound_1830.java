package rails.game;

import rails.common.DisplayBuffer;
import rails.common.GameOption;
import rails.common.LocalText;
import rails.common.ReportBuffer;
import rails.game.action.*;
import rails.game.state.ChangeStack;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_1830 extends StartRound {
    int bidIncrement;

    /**
     * Constructed via Configure
     */
    public StartRound_1830(GameManager parent, String id) {
        super(parent, id);
        hasBidding = true;
        bidIncrement = startPacket.getModulus();
    }
    
    /**
     * Start the 1830-style start round.
     *
     * @param startPacket The startpacket to be sold in this start round.
     */
    @Override
    public void start() {
        super.start();
        setPossibleActions();

    }

    @Override
    public boolean process(PossibleAction action) {

        if (!super.process(action)) return false;

        // Assign any further items that have been bid exactly once
        // and don't need any further player intervention, such
        // as setting a start price
        StartItem item;
        while ((item = startPacket.getFirstUnsoldItem()) != null
                && item.getBidders() == 1 && item.needsPriceSetting() == null) {
            assignItem(item.getBidder(), item, item.getBid(), 0);

            // Check if this has exhausted the start packet
            if (startPacket.areAllSold()) {
                finishRound();
                break;
            }
        }
        return true;
    }

    @Override
    public boolean setPossibleActions() {

        boolean passAllowed = true;

        possibleActions.clear();

        if (currentPlayer == startPlayer) ReportBuffer.add(this, "");

        while (possibleActions.isEmpty()) {

            Player currentPlayer = getCurrentPlayer();

            for (StartItem item : itemsToSell.view()) {

                if (item.isSold()) {
                    // Don't include
                } else if (item.getStatus() == StartItem.AUCTIONED) {

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
                } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                    /* This status is set in buy() if a share price is missing */
                    setPlayer(item.getBidder());
                    possibleActions.add(new BuyStartItem(item, item.getBid(), false, true));
                    passAllowed = false;
                    break; // No more actions
                } else if (item == startPacket.getFirstUnsoldItem()) {
                    if (item.getBidders() == 1) {
                        // Bid upon by one player.
                        // If we need a share price, ask for it.
                        PublicCompany comp = item.needsPriceSetting();
                        if (comp != null) {
                            setPlayer(item.getBidder());
                            item.setStatus(StartItem.NEEDS_SHARE_PRICE);
                            BuyStartItem newItem =
                                    new BuyStartItem(item, item.getBasePrice(),
                                            true, true);
                            possibleActions.add(newItem);
                            break; // No more actions possible!
                        } else {
                            // ERROR, this should have been detected in process()!
                            log.error("??? Wrong place to assign item "+item.getName());
                            assignItem(item.getBidder(), item, item.getBid(), 0);
                        }
                    } else if (item.getBidders() > 1) {
                        ReportBuffer.add(this, LocalText.getText("TO_AUCTION",
                                item.getName()));
                        // Start left of the currently highest bidder
                        if (item.getStatus() != StartItem.AUCTIONED) {
                            setNextBiddingPlayer(item,
                                    item.getBidder().getIndex());
                            currentPlayer = getCurrentPlayer();
                            item.setStatus(StartItem.AUCTIONED);
                            auctionItemState.set(item);
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

            if (possibleActions.isEmpty()) {
                numPasses.add(1);
                if (auctionItemState.value() == null) {
                    setNextPlayer();
                } else {
                    setNextBiddingPlayer((StartItem) auctionItemState.value());
                }
            }
        }

        if (passAllowed) {
            possibleActions.add(new NullAction(NullAction.PASS));
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
        Player player = getCurrentPlayer();
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
                errMsg = LocalText.getText("BidTooHigh", Currency.format(this, available));
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

        ChangeStack.start(this, bidItem);

        item.setBid(bidAmount, player);
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Currency.format(this, bidAmount),
                item.getName(),
                Currency.format(this, player.getFreeCash()) ));

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
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

        String errMsg = null;
        Player player = getCurrentPlayer();
        StartItem auctionItem = (StartItem) auctionItemState.value();

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

        ChangeStack.start(this, action);

        numPasses.add(1);
        if (auctionItem != null) {

            if (numPasses.value() >= auctionItem.getBidders() - 1) {
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
                setPriorityPlayer(); // EV - Added to fix bug 2989440
            } else {
                // More than one left: find next bidder

                if (GameOption.getAsBoolean(this, "LeaveAuctionOnPass")) {
                    // Game option: player to leave auction after a pass (default no).
                    player.unblockCash(auctionItem.getBid(player));
                    auctionItem.setBid(-1, player);
                }

                setNextBiddingPlayer(auctionItem,
                        getCurrentPlayerIndex());
            }

        } else {

            if (numPasses.value() >= numPlayers) {
                // All players have passed.
                ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
                // It the first item has not been sold yet, reduce its price by
                // 5.
                if (startPacket.getFirstUnsoldItem() == startPacket.getFirstItem()) {
                    startPacket.getFirstItem().reduceBasePriceBy(5);
                    ReportBuffer.add(this, LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                                    startPacket.getFirstItem().getName(),
                                    Currency.format(this, startPacket.getFirstItem().getBasePrice()) ));
                    numPasses.set(0);
                    if (startPacket.getFirstItem().getBasePrice() == 0) {
                        assignItem(getCurrentPlayer(),
                                startPacket.getFirstItem(), 0, 0);
                        getRoot().getPlayerManager().setPriorityPlayer();
                        // startPacket.getFirstItem().getName());
                    } else {
                        //BR: If the first item's price is reduced, but not to 0, 
                        //    we still need to advance to the next player
                        setNextPlayer();
                    }
                } else {
                    numPasses.set(0);
                    //gameManager.nextRound(this);
                    finishRound();

                }
//            } else if (auctionItem != null) {
                // TODO  Now dead code - should it be reactivated?
//                setNextBiddingPlayer(auctionItem);
            } else {
                setNextPlayer();
            }
        }

        return true;
    }

    private void setNextBiddingPlayer(StartItem item, int currentIndex) {
        for (int i = currentIndex + 1; i < currentIndex
                                           + getRoot().getPlayerManager().getNumberOfPlayers(); i++) {
            if (item.hasBid(getRoot().getPlayerManager().getPlayerByIndex(i))) {
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
        return "1830 Start Round help text";
    }

}
