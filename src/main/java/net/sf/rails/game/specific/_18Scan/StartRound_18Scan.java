package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.*;
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
    protected final BooleanState buying = new BooleanState (this, "biddingOrBuying", false);
    protected final IntegerState currentBid = IntegerState.create (this, "CurrentBid", 0);
    int minimumInitialBid;
    int minimumIncrement;
    int modulus;
    IntegerState minimumBid = IntegerState.create (this, "minimumBid", 0);
    HashMapState<Player, Boolean> isActive = HashMapState.create (this, "isPlayerBidding");
    GenericState<Player> lastBiddingPlayer = new GenericState<>(this, "lastBiddingPlayer");

    /**
     * Constructed via Configure
     */
    public StartRound_18Scan(GameManager parent, String id) {
        super(parent, id, Bidding.ON_BUY_RIGHT, true, true);
        minimumInitialBid = startPacket.getMinimumInitialIncrement(); // Should be 0 here
        minimumIncrement = startPacket.getMinimumIncrement(); // Should be 5 here
        modulus = startPacket.getModulus(); // Should be 5 here
    }

    @Override
    public void start() {
        super.start();
        for (StartItem item : itemsToSell) {
            item.setStatus(StartItem.BUYABLE);
        }
        initBidding();
    }

    private void initBidding () {
        for (Player player : playerManager.getPlayers()) {
            isActive.put(player, true);
            player.blockCash(-player.getBlockedCash()); // TODO Not nice
            player.getBlockedCashModel().setSuppressZero(true);
        }
        buying.set(false);
        lastBiddingPlayer.set (null);
        minimumBid.set (minimumInitialBid - minimumIncrement); // TODO Reorganize this
        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {

        Player currentPlayer = playerManager.getCurrentPlayer();
        boolean passAllowed = false;

        possibleActions.clear();

        // TODO Why this?
        if (currentPlayer == startPlayer) ReportBuffer.add(this, "");

        if (buying.value()) {
            // Select any unsold item
            for (StartItem item : itemsToSell.view()) {

                if (item.isSold()) {
                    // Don't include
                } else if (currentPlayer.getFreeCash() >= item.getBasePrice()) {
                    BuyStartItem possibleAction =
                            new BuyStartItem(item, item.getBasePrice(), false);
                    possibleActions.add(possibleAction);
                } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                    /* This status is set in buy() if a share price is missing */
                    possibleActions.add(new BuyStartItem(item, item.getBasePrice(), false, true));
                    passAllowed = false;
                    break; // No more actions
                } else {
                    if (currentPlayer.getFreeCash() >= item.getBasePrice()) {
                        item.setStatus(StartItem.BUYABLE);
                        possibleActions.add(new BuyStartItem(item,
                                item.getBasePrice(), false));
                    }
                }
            }
            passAllowed = false;

        } else {
            // Bidding
            if (currentPlayer.getFreeCash() >= minimumBid.value()) {
                    BidStartItem possibleAction =
                            new BidStartItem(getRoot(), minimumBid.value(),
                                    minimumIncrement, true);
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
            passAllowed = true;
        }

        if (passAllowed) {
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
        }

        return true;
    }

    @Override
    public boolean process(PossibleAction action) {

        if (action instanceof BidStartItem) {

            return bid(action.getPlayerName(), (BidStartItem) action);

        } else if (action instanceof BuyStartItem) {

            return buy(action.getPlayerName(), (BuyStartItem) action);

        } else if (action instanceof NullAction) {

            return pass((NullAction) action, action.getPlayerName());

        } else {
            log.error ("Unexpected action: {}", action.toString());
            return false;
        }
    }
    /*----- moveStack methods -----*/
    /**
     * The current player bids on a given start item.
     *
     * @param bidItem The name of the start item on which the bid is placed.
      */
    protected boolean bid (String playerName, BidStartItem bidItem) {

        String errMsg = null;
        int previousBid = 0;
        Player currentPlayer = playerManager.getCurrentPlayer();
        int bidAmount = bidItem.getActualBid();
        Player player = bidItem.getPlayer();

        while (true) {

            // Check player
            if (!player.equals(currentPlayer)) {
                errMsg = LocalText.getText("WrongPlayer", player, currentPlayer.getId());
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
            // Bid must be at least 5 above last bid
            if (bidAmount < minimumBid.value()) {
                errMsg = LocalText.getText("BidTooLow", bidAmount);
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidAmount % modulus != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf",
                        bidAmount,
                        modulus);
                break;
            }

            // Has the buyer enough cash?
            if (bidAmount > currentPlayer.getFreeCash()) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(this, bidAmount));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    player.getId(),
                    bidItem.toString(),
                    errMsg ));
            return false;
        }

        currentBid.set(bidAmount);
        lastBiddingPlayer.set(currentPlayer);
        // We want to see the initial zero bid!
        if (bidAmount == 0) currentPlayer.getBlockedCashModel().setSuppressZero(false);
        previousBid = currentPlayer.getBlockedCash();
        // TODO: check blocked cash logic in Player, we might not need both blocked and free cash
        if (previousBid > 0) currentPlayer.unblockCash(previousBid);
        currentPlayer.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_BUY_RIGHT_LOG",
                playerName,
                Bank.format(this, bidAmount),
                Bank.format(this, currentPlayer.getFreeCash()) ));

        setNextBiddingPlayer(currentPlayer);
        minimumBid.set(bidAmount + minimumIncrement);
        numPasses.set(0);

        return true;

    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {

        boughtItem.select();
        boolean result = super.buy(playerName, boughtItem);

        int numSold = 0;
        for (StartItem item : itemsToSell) {
            if (item.isSold()) numSold++;
        }
        if (numSold == itemsToSell.size() || lastBiddingPlayer.value() == null) {
            // All sold, or nobody wanted to bid
            finishRound();
        } else {
            initBidding();
        }
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
        isActive.put(player, false);

        int remainingBidders = playerManager.getNumberOfPlayers() - numPasses.value();
        if (lastBiddingPlayer.value() == null && remainingBidders == 0) {
            // Nobody wants to bid.
            // The first time this happens, the priority holder is obliged to buy,
            // assuming a bid of zero, and then the round ends.
            // Otherwise the round ends immediately.
            playerManager.setCurrentPlayer(playerManager.getPriorityPlayer());
            if (gameManager.getAbsoluteORNumber() == 0) {
                finishBidding(true);
            } else {
                // To get two ORs, we need to tweak GameManager a bit.
                gameManager.setShortOR (true);
                finishRound();
                return true;
            }
        } else if (lastBiddingPlayer.value() != null && remainingBidders == 1) {
            // One bidder remains and has won the right to buy
            playerManager.setCurrentPlayer(lastBiddingPlayer.value());
            finishBidding(false);
        } else {
            setNextBiddingPlayer (player);
        }
        setPossibleActions();
        return true;
    }

    private void finishBidding(boolean forcedBuy) {
        gameManager.reportAllPlayersPassed();
        Player player = playerManager.getCurrentPlayer();
        if (forcedBuy) {
           ReportBuffer.add(this, LocalText.getText("IsForcedToBuyItem",
                    player.getId()));
        } else {
            int amount = currentBid.value();
            String priceText = Currency.toBank(player, amount);
            ReportBuffer.add(this, LocalText.getText("PaysForBuyRight",
                    player.getId(),
                    priceText));
        }

        buying.set(true);
        playerManager.setPriorityPlayerToNext();
    }

    private void setNextBiddingPlayer(Player currentPlayer) {
        for (Player player:playerManager.getNextPlayersAfter(currentPlayer, false, false)) {
            if (isActive.get(player)) {
                playerManager.setCurrentPlayer(player);
                break;
            }
        }
    }


}
