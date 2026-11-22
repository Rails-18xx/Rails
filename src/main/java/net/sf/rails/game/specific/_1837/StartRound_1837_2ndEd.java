package net.sf.rails.game.specific._1837;

import net.sf.rails.game.state.BooleanState;
import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;

/**
 * Refactored StartRound for 1837 2nd Edition.
 * Logic optimized for state clarity and removal of anti-patterns.
 */
public class StartRound_1837_2ndEd extends StartRound {

    public static final int SELECT_STEP = 0;
    public static final int OPEN_STEP = 1;
    public static final int BUY_STEP = 2;
    public static final int BID_STEP = 3;

    protected final IntegerState currentStep = IntegerState.create(this, "currentStep", SELECT_STEP);
    protected final GenericState<Player> selectingPlayer = new GenericState<>(this, "selectingPlayer");
    protected final IntegerState currentBuyPrice = IntegerState.create(this, "currentBuyPrice");
    protected final GenericState<StartItem> currentAuctionItem = new GenericState<>(this, "currentAuctionItem");
    protected final IntegerState numPasses = IntegerState.create(this, "numberOfPasses", 0);
private boolean buyOnly = false;
    /**
     * Constructed via Configure
     */
    public StartRound_1837_2ndEd(GameManager parent, String id) {
        super(parent, id, true, true, false);
    }

    protected StartRound_1837_2ndEd(GameManager parent, String id,
                        Bidding hasBidding, boolean hasBasePrices, boolean hasBuying) {
        super(parent, id, hasBidding, hasBasePrices, hasBuying);
    }

    @Override
    public void start() {
        super.start();
        // Removed unused 'buyOnly' assignment.

        buyOnly = ((GameManager_1837)gameManager).isBuyOnly();
        currentStep.set(SELECT_STEP);
        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        Player currentPlayer = playerManager.getCurrentPlayer();

        switch (currentStep.value()) {
            case SELECT_STEP:
                setupSelectionStep(currentPlayer);
                break;
            case BUY_STEP:
                setupBuyStep(currentPlayer);
                break;
            case OPEN_STEP:
            case BID_STEP:
                setupBiddingStep(currentPlayer);
                break;
        }
        return true;
    }

    private void setupSelectionStep(Player currentPlayer) {
        selectingPlayer.set(currentPlayer);
        currentBuyPrice.set(100);

        for (StartItem item : itemsToSell.view()) {
            if (item.isSold()) continue;

            item.setStatus(StartItem.SELECTABLE);

            if (hasBuying) {
                possibleActions.add(new BuyStartItem(item, item.getBasePrice(), false));
            } else {
                item.setMinimumBid(item.getBasePrice());
                // In v2.0, selecting an item initiates the auction process
                possibleActions.add(new BidStartItem(item, item.getBasePrice(),
                        startPacket.getModulus(), false, true));
            }
        }
        possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
    }

    private void setupBuyStep(Player currentPlayer) {
        if (currentBuyPrice.value() <= currentPlayer.getFreeCash()) {
            possibleActions.add(new BuyStartItem(
                    currentAuctionItem.value(),
                    currentBuyPrice.value(), true));
        }
        possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
    }

    private void setupBiddingStep(Player currentPlayer) {
        StartItem item = currentAuctionItem.value();
        if (item.getMinimumBid() <= currentPlayer.getFreeCash()) {
            possibleActions.add(new BidStartItem(item, item.getMinimumBid(),
                    startPacket.getModulus(), true));
        }

        NullAction passAction = new NullAction(getRoot(), NullAction.Mode.PASS);
        if (currentStep.value() == OPEN_STEP) {
            passAction.setLabel("DeclineToBid");
        }
        possibleActions.add(passAction);
    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();
        int status = boughtItem.getStatus();
        Player player = playerManager.getCurrentPlayer();
        int price;

        // --- Guard Clauses for Validation ---

        if (status == StartItem.AUCTIONED && currentStep.value() == BUY_STEP) {
            price = currentBuyPrice.value();
        } else {
            return reportError(playerName, item, LocalText.getText("NotForSale"));
        }

        if (player.getFreeCash() < price) {
            return reportError(playerName, item, LocalText.getText("NoMoney"));
        }

        // --- Execution ---

        assignItem(player, item, price, 0);
        ((PublicCertificate) item.getPrimary()).getCompany().start();

        getRoot().getPlayerManager().setPriorityPlayerToNext();
        setNextSelectingPlayer();
        numPasses.set(0);
        currentStep.set(SELECT_STEP);

        return true;
    }

    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {
        StartItem item = bidItem.getStartItem();
        StartItem auctionedItem = currentAuctionItem.value();
        Player player = playerManager.getCurrentPlayer();
        int bidAmount = bidItem.getActualBid();

        // --- Guard Clauses for Validation ---

        // 1. Check if this is a selection (-1) or a real bid
        if (bidAmount == -1) {
            if (!bidItem.isSelectForAuction()) {
                return reportError(playerName, item, LocalText.getText("NotBiddable"));
            }
        } else {
            // 2. Real Bid Validations
            if ((currentStep.value() == OPEN_STEP || currentStep.value() == BID_STEP)
                    && !item.equals(auctionedItem)) {
                return reportError(playerName, item, LocalText.getText("WrongStartItem", item.getId(), auctionedItem.getId()));
            }

            if (bidAmount < item.getMinimumBid()) {
                return reportError(playerName, item, LocalText.getText("BidTooLow", Bank.format(this, item.getMinimumBid())));
            }

            if (bidAmount % startPacket.getModulus() != 0) {
                return reportError(playerName, item, LocalText.getText("BidMustBeMultipleOf", bidAmount, startPacket.getMinimumIncrement()));
            }

            if (bidAmount > player.getCash()) {
                return reportError(playerName, item, LocalText.getText("BidTooHigh", Bank.format(this, bidAmount)));
            }
        }

        // --- Execution ---

        if (currentStep.value() == SELECT_STEP) {
            startAuctionFor(item, playerName, bidAmount);
        }

        if (bidAmount > 0) {
            placeBid(item, player, bidAmount, playerName);
        }

        processPostBidState(item);

        return true;
    }

    private void startAuctionFor(StartItem item, String playerName, int bidAmount) {
        currentAuctionItem.set(item);
        item.setStatus(StartItem.AUCTIONED);
        item.setAllActive();

        // Disable other items
        for (StartItem otherItem : itemsToSell.view()) {
            if (otherItem != item && !otherItem.isSold()) {
                otherItem.setStatus(StartItem.UNAVAILABLE);
            }
        }

        if (bidAmount == -1) {
            currentStep.set(OPEN_STEP);
        }

        ReportBuffer.add(this, " ");
        ReportBuffer.add(this, LocalText.getText("SelectForAuctioning", playerName, item.getId()));
    }

    private void placeBid(StartItem item, Player player, int bidAmount, String playerName) {
        item.setBid(bidAmount, player);
        item.setMinimumBid(bidAmount + 5);
        currentStep.set(BID_STEP);

        ReportBuffer.add(this, LocalText.getText("BID_ITEM",
                playerName,
                Bank.format(this, bidAmount),
                item.getId()));
    }

    private void processPostBidState(StartItem item) {
        switch (currentStep.value()) {
            case OPEN_STEP:
            case BUY_STEP:
                // Move to next player. If we wrap around to the selector, handle forced buy logic.
                Player currentPlayer = playerManager.setCurrentToNextPlayer();
                if (currentPlayer == selectingPlayer.value()) {
                    handlePriceDrop(item, currentPlayer);
                }
                break;

            case BID_STEP:
                setNextBiddingPlayer();
                break;
        }
    }

    @Override
    public boolean pass(NullAction action, String playerName) {
        StartItem auctionedItem = currentAuctionItem.value();

        switch (currentStep.value()) {
            case OPEN_STEP:
            case BUY_STEP:
                ReportBuffer.add(this, LocalText.getText("DeclinedToBid", playerName));
                Player currentPlayer = playerManager.setCurrentToNextPlayer();

                // If everyone passed, drop price or force buy
                if (currentPlayer == selectingPlayer.value()) {
                    handlePriceDrop(auctionedItem, currentPlayer);
                }
                break;

            case BID_STEP:
                ReportBuffer.add(this, LocalText.getText("PASSES", playerName));
                auctionedItem.setPass(playerManager.getCurrentPlayer());

                setNextBiddingPlayer();
                if (playerManager.getCurrentPlayer() == auctionedItem.getBidder()) {
                    completeAuction(auctionedItem);
                }
                break;

            case SELECT_STEP:
                numPasses.add(1);
                if (numPasses.value() == playerManager.getNumberOfPlayers()) {
                    finishRound();
                } else {
                    setNextSelectingPlayer();
                }
                break;
        }

        return true;
    }

    /**
     * Handles the logic when all players pass on an item in the OPEN or BUY steps.
     * Decreases price, checks for 0-price forced buy, or resets to BUY_STEP.
     */
    private void handlePriceDrop(StartItem auctionedItem, Player currentPlayer) {
        currentBuyPrice.add(-10);

        if (currentBuyPrice.value() <= 0) {
            // Forced Buy Condition
            // Set bid to 0 purely for transaction logging consistency
            auctionedItem.setBid(0, currentPlayer);
            assignItem(currentPlayer, auctionedItem, 0, 0);

            // Reset for next item
            currentStep.set(SELECT_STEP);
            setNextSelectingPlayer();
        } else {
            // Price Reduced, offer again
            auctionedItem.setMinimumBid(currentBuyPrice.value());
            ReportBuffer.add(this, LocalText.getText("ITEM_PRICE_REDUCED",
                    auctionedItem.getId(),
                    Bank.format(this, currentBuyPrice.value())));
            currentStep.set(BUY_STEP);
        }
    }

    private void completeAuction(StartItem item) {
        assignItem(item.getBidder(), item, item.getBid(), 0);
        currentStep.set(SELECT_STEP);
        setNextSelectingPlayer();
        numPasses.set(0);
    }

    private void setNextBiddingPlayer() {
        Player currentPlayer;
        do {
            currentPlayer = playerManager.setCurrentToNextPlayer();
        } while (!currentAuctionItem.value().isActive(currentPlayer));
    }

    protected void setNextSelectingPlayer() {
        playerManager.setCurrentToNextPlayerAfter(selectingPlayer.value());
    }

    /**
     * Helper to report errors to the DisplayBuffer and return false.
     */
    private boolean reportError(String playerName, StartItem item, String errorDetails) {
        DisplayBuffer.add(this, LocalText.getText("CantBuyItem", // Reusing generic error key or "InvalidBid" based on context
                playerName,
                item != null ? item.getId() : "?",
                errorDetails));
        return false;
    }
}