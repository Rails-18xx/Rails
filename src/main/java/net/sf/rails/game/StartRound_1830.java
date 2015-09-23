package net.sf.rails.game;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.GenericState;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_1830 extends StartRound {
    protected final int bidIncrement;
    
    private final GenericState<StartItem> auctionItemState = 
            GenericState.create(this, "auctionItemState");

    /**
     * Constructed via Configure
     */
    public StartRound_1830(GameManager parent, String id) {
        super(parent, id);
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

        if (playerManager.getCurrentPlayer() == startPlayer) ReportBuffer.add(this, "");

        // FIXME: Rails 2.0 Could be an infinite loop if there if no player has enough money to buy an item
        while (possibleActions.isEmpty()) {
            Player currentPlayer = playerManager.getCurrentPlayer();

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
                    playerManager.setCurrentPlayer(item.getBidder());
                    possibleActions.add(new BuyStartItem(item, item.getBid(), false, true));
                    passAllowed = false;
                    break; // No more actions
                } else if (item == startPacket.getFirstUnsoldItem()) {
                    if (item.getBidders() == 1) {
                        // Bid upon by one player.
                        // If we need a share price, ask for it.
                        PublicCompany comp = item.needsPriceSetting();
                        if (comp != null) {
                            playerManager.setCurrentPlayer(item.getBidder());
                            item.setStatus(StartItem.NEEDS_SHARE_PRICE);
                            BuyStartItem newItem =
                                    new BuyStartItem(item, item.getBasePrice(),
                                            true, true);
                            possibleActions.add(newItem);
                            break; // No more actions possible!
                        } else {
                            // ERROR, this should have been detected in process()!
                            log.error("??? Wrong place to assign item "+item.getId());
                            assignItem(item.getBidder(), item, item.getBid(), 0);
                        }
                    } else if (item.getBidders() > 1) {
                        ReportBuffer.add(this, LocalText.getText("TO_AUCTION",
                                item.getId()));
                        // Start left of the currently highest bidder
                        if (item.getStatus() != StartItem.AUCTIONED) {
                            setNextBiddingPlayer(item, item.getBidder());
                            currentPlayer = playerManager.getCurrentPlayer();
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
                    playerManager.setCurrentToNextPlayer();
                } else {
                    setNextBiddingPlayer(auctionItemState.value());
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
                    item.getId(),
                    errMsg ));
            return false;
        }

        

        item.setBid(bidAmount, player);
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(this, bidAmount),
                item.getId(),
                Bank.format(this, player.getFreeCash()) ));

        if (bidItem.getStatus() != StartItem.AUCTIONED) {
            playerManager.setPriorityPlayerToNext();
            playerManager.setCurrentToNextPlayer();
        } else {
            setNextBiddingPlayer(item);
        }
        numPasses.set(0);

        return true;

    }
    
    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        boolean result = super.buy(playerName, boughtItem);
        auctionItemState.set(null);
        return result;
    }
    
    
        /**
         * Process a player's pass.
         * @param playerName The name of the current player (for checking purposes).
         */
        @Override
        protected boolean pass(NullAction action, String playerName) {
    
            String errMsg = null;
            Player player = playerManager.getCurrentPlayer();
            StartItem auctionItem = auctionItemState.value();
    
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
                    playerManager.setCurrentToPriorityPlayer(); // EV - Added to fix bug 2989440
                } else {
                    // More than one left: find next bidder
    
                    if (GameOption.getAsBoolean(this, "LeaveAuctionOnPass")) {
                        // Game option: player to leave auction after a pass (default no).
                        player.unblockCash(auctionItem.getBid(player));
                        auctionItem.setPass(player);
                    }
                    setNextBiddingPlayer(auctionItem);
                }
    
            } else {
    
                if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
                    // All players have passed.
                    gameManager.reportAllPlayersPassed();
                    // It the first item has not been sold yet, reduce its price by 5.
                    if (startPacket.getFirstItem() == startPacket.getFirstUnsoldItem() || startPacket.getFirstUnsoldItem().getReduceable()) {
                        startPacket.getFirstUnsoldItem().reduceBasePriceBy(5);
                        ReportBuffer.add(this, LocalText.getText(
                                "ITEM_PRICE_REDUCED",
                                        startPacket.getFirstUnsoldItem().getId(),
                                        Bank.format(this, startPacket.getFirstUnsoldItem().getBasePrice()) ));
                        numPasses.set(0);
                        if (startPacket.getFirstUnsoldItem().getBasePrice() == 0) {
                            getRoot().getPlayerManager().setCurrentToNextPlayer();
                            assignItem(playerManager.getCurrentPlayer(),
                                    startPacket.getFirstUnsoldItem(), 0, 0);
                            getRoot().getPlayerManager().setPriorityPlayerToNext();
                            getRoot().getPlayerManager().setCurrentToNextPlayer();
                        } else {
                            //BR: If the first item's price is reduced, but not to 0, 
                            //    we still need to advance to the next player
                            playerManager.setCurrentToNextPlayer();
                        }
                    } else {
                        numPasses.set(0);
                        //gameManager.nextRound(this);
                        finishRound();
    
                    }
                } else {
                    playerManager.setCurrentToNextPlayer();
                }
            }
    
            return true;
        }

    
    private void setNextBiddingPlayer(StartItem item, Player biddingPlayer) {
        for (Player player:playerManager.getNextPlayersAfter(biddingPlayer, false, false)) {
            if (item.isActive(player)) {
                playerManager.setCurrentPlayer(player);
                break;
            }
        }
    }
    
    private void setNextBiddingPlayer(StartItem item) {
        setNextBiddingPlayer(item, playerManager.getCurrentPlayer());
    }

}
