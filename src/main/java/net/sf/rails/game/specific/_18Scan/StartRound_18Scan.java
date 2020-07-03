package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.GenericState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

public class StartRound_18Scan  extends StartRound {
    private static final Logger log = LoggerFactory.getLogger(StartRound_18Scan.class);

    /**
     * The 18Scan StartRound has two non-overlapping states:
     * - bidding, on the right to buy the next item, and
     * - buying, choose any unsold start packet item.
     */
    private boolean buying;
    int currentBid;
    int minimumBid;
    int bidIncrement;
    int modulus;

    // TEMP. NOTE: initially everything was copied from 1830

    /**
     * Constructed via Configure
     */
    public StartRound_18Scan(GameManager parent, String id) {
        super(parent, id);
        modulus = startPacket.getModulus();
    }

    @Override
    public void start() {
        super.start();
        //auctionItemState.set(null);
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

        boolean passAllowed = false;

        possibleActions.clear();

        Player player = playerManager.getCurrentPlayer();
        if (player == startPlayer) ReportBuffer.add(this, "");  //??

        if (buying) {
            // Select any unsold item
            for (StartItem item : itemsToSell.view()) {

                if (item.isSold()) {
                    // Don't include
                } else if (player.getFreeCash() >= item.getBasePrice()) {
                    BuyStartItem possibleAction =
                            new BuyStartItem(item, item.getBasePrice(), false);
                    possibleActions.add(possibleAction);
                } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                    /* This status is set in buy() if a share price is missing */
                    possibleActions.add(new BuyStartItem(item, item.getBasePrice(), false, true));
                    passAllowed = false;
                    break; // No more actions
                } else {
                    if (player.getFreeCash() >= item.getBasePrice()) {
                        item.setStatus(StartItem.BUYABLE);
                        possibleActions.add(new BuyStartItem(item,
                                item.getBasePrice(), false));
                    }
                }
            }


        } else {
            // Bidding
            int minimumBid = currentBid + bidIncrement;
            if (player.getFreeCash() >= minimumBid) {
                    BidStartItem possibleAction =
                            new BidStartItem(null, minimumBid,
                                    modulus, false);
                    possibleActions.add(possibleAction);
            }


            if (possibleActions.isEmpty()) {
                numPasses.add(1);
                //if (auctionItemState.value() == null) {
                //    playerManager.setCurrentToNextPlayer();
                //} else {
                //    setNextBiddingPlayer(auctionItemState.value());
                //}
            }
        }

        if (passAllowed) {
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
        }

        return true;
    }

    /*----- moveStack methods -----*/
    /**
     * The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param bidItem The name of the start item on which the bid is placed.
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
        //auctionItemState.set(null);
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
        //StartItem auctionItem = auctionItemState.value();

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
