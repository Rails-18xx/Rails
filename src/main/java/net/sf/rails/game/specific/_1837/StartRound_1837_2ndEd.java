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

    /**
     * True if the first start round has finished, either complete or not.
     * In version 2, any subsequent start rounds are officially stock rounds
     * (only buying at list price, no bidding).
     * If in such a subsequent round the start packet is completely sold,
     * it will not be followed by an operating round but by a stock round.
     */
    private boolean buyOnly = false;

    /**
     * Constructed via Configure
     */
    public StartRound_1837_2ndEd(GameManager parent, String id) {
        super(parent, id, true, true, false);
        // bidding, with base prices
    }

    /**
     * A pass-through for subclass StartRound_1837_2ndEd_buying
     * @param parent
     * @param id
     * @param hasBidding
     * @param hasBasePrices
     * @param hasBuying
     */
    protected StartRound_1837_2ndEd(GameManager parent, String id,
                        Bidding hasBidding, boolean hasBasePrices, boolean hasBuying) {
        super(parent, id, hasBidding, hasBasePrices, hasBuying);

    }

    @Override
    public void start() {
        super.start();
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
            // In the selection step, all not yet sold items are selectable.
            // The current player MUST select an item,
            // and may then bid for it or pass.
            // NO! Passing is allowed as per the v2.0 rules (2015, English Version by Lonny).

            selectingPlayer.set(currentPlayer);
            currentBuyPrice.set(100);

            for (StartItem item : itemsToSell.view()) {
                if (!item.isSold()) {
                    if (hasBuying) {
                        item.setStatus(StartItem.SELECTABLE);
                        BuyStartItem possibleAction =
                                new BuyStartItem (item, item.getBasePrice(), false);
                        possibleActions.add(possibleAction);
                    } else {
                        item.setStatus(StartItem.SELECTABLE);
                        item.setMinimumBid(item.getBasePrice());
                        BidStartItem possibleAction =
                                new BidStartItem(item, item.getBasePrice(),
                                        startPacket.getModulus(), false, true);
                        possibleActions.add(possibleAction);
                    }
                }
            }
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
            break;
        case BUY_STEP:
            // only offer buy if enough money
            if (currentBuyPrice.value() <= currentPlayer.getFreeCash()) {
                possibleActions.add(new BuyStartItem(
                        currentAuctionItem.value(),
                        currentBuyPrice.value(), true));
            }
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
            break;
        case OPEN_STEP:
        case BID_STEP:
            StartItem item = currentAuctionItem.value();
            // only offer if enough money
            if (item.getMinimumBid() <= currentPlayer.getFreeCash()) {
                BidStartItem possibleAction =
                    new BidStartItem(item, item.getMinimumBid(),
                            startPacket.getModulus(), true);
                possibleActions.add(possibleAction);
            }
            if (currentStep.value() == OPEN_STEP) {
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS).setLabel("DeclineToBid"));
            } else {
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
            }
            break;
        }

        return true;
    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();
        int status = boughtItem.getStatus();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int price = 0;

        while (true) {

            // Is the item buyable?
            if (status == StartItem.AUCTIONED && currentStep.value() == BUY_STEP) {
                price = currentBuyPrice.value();
            } else {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            if (player.getFreeCash() < price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName,
                    item.getId(),
                    errMsg ));
            return false;
        }

        assignItem(player, item, price, 0);
        ((PublicCertificate) item.getPrimary()).getCompany().start();
        getRoot().getPlayerManager().setPriorityPlayerToNext();
        setNextSelectingPlayer();
        numPasses.set (0);
        currentStep.set(SELECT_STEP);

        return true;
    }

    /**
     * The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param bidItem The  start item on which the bid is placed.
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        StartItem auctionedItem = currentAuctionItem.value();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // If the bidAmount is -1, this is just a selection for auctioning
            if (bidAmount == -1) {
                if (!bidItem.isSelectForAuction()) {
                    errMsg = LocalText.getText("NotBiddable");
                    break;
                }
            } else {

                // If auctioning, must be the right item
                if ((currentStep.value() == OPEN_STEP || currentStep.value() == BID_STEP)
                    && !item.equals(auctionedItem)) {
                    errMsg =
                            LocalText.getText("WrongStartItem",
                                    item.getId(),
                                    auctionedItem.getId() );
                    break;
                }

                // Bid must be at least the minimum bid
                if (bidAmount < item.getMinimumBid()) {
                    errMsg =
                            LocalText.getText("BidTooLow",
                                    Bank.format(this, item.getMinimumBid()));
                    break;
                }

                // Bid must be a multiple of the modulus
                if (bidAmount % startPacket.getModulus() != 0) {
                    errMsg = LocalText.getText(
                                    "BidMustBeMultipleOf",
                                    bidAmount,
                                    startPacket.getMinimumIncrement() );
                    break;
                }

                // Player must have enough cash
                if (bidAmount > player.getCash()) {
                    errMsg =
                            LocalText.getText("BidTooHigh",
                                    Bank.format(this, bidAmount));
                    break;
                }
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

        if (currentStep.value() == SELECT_STEP) {

            currentAuctionItem.set(item);
            item.setStatus(StartItem.AUCTIONED);
            item.setAllActive();

            for (StartItem item2 : itemsToSell.view()) {
                if (item2 != item && !item2.isSold()) {
                    item2.setStatus(StartItem.UNAVAILABLE);
                }
            }
            if (bidAmount == -1) {
                currentStep.set(OPEN_STEP);
            }

            ReportBuffer.add(this, " ");
            ReportBuffer.add(this, LocalText.getText("SelectForAuctioning",
                    playerName,
                    item.getId() ));
        }

        if (bidAmount > 0) {
            item.setBid(bidAmount, player);
            item.setMinimumBid(bidAmount + 5);
            currentStep.set(BID_STEP);

            ReportBuffer.add(this, LocalText.getText("BID_ITEM",
                    playerName,
                    Bank.format(this, bidAmount),
                    item.getId() ));
        }

        switch (currentStep.value()) {
        case OPEN_STEP:
        case BUY_STEP:
            Player currentPlayer = playerManager.setCurrentToNextPlayer();
            if (currentPlayer == selectingPlayer.value()) {
                // All have passed, now lower the buy price
                currentBuyPrice.add(-10);
                currentStep.set(BUY_STEP);
                if (currentBuyPrice.value() == 0) {
                    // Forced buy
                    assignItem(currentPlayer, item, 0, 0);
                }
            }
            break;

        case BID_STEP:
            setNextBiddingPlayer();
        }

        return true;

    }

    /**
     * Process a player's pass.
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    public boolean pass(NullAction action, String playerName) {
        // All validations have already been done

        StartItem auctionedItem = currentAuctionItem.value();

        switch (currentStep.value()) {
        case OPEN_STEP:
        case BUY_STEP:
            ReportBuffer.add(this, LocalText.getText("DeclinedToBid", playerName));

            Player currentPlayer = playerManager.setCurrentToNextPlayer();
            if (currentPlayer == selectingPlayer.value()) {
                // All have passed, now lower the buy price
                currentBuyPrice.add(-10);
                auctionedItem.setMinimumBid(currentBuyPrice.value());
                ReportBuffer.add(this, LocalText.getText("ITEM_PRICE_REDUCED",
                        auctionedItem.getId(),
                        Bank.format(this, currentBuyPrice.value()) ));
                currentStep.set(BUY_STEP);

                if (currentBuyPrice.value() == 0) {
                    // Forced buy
                    // Trick to make the zero buy price visible
                    auctionedItem.setBid(0, currentPlayer);

                    assignItem(currentPlayer, auctionedItem, 0, 0);
                    currentStep.set(SELECT_STEP);
                    setNextSelectingPlayer();
                }
            }
            break;

        case BID_STEP:
            ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

            auctionedItem.setPass(playerManager.getCurrentPlayer());

            // We are done if the next still bidding player
            // is equal to the current highest bidder.
            setNextBiddingPlayer();
            if (playerManager.getCurrentPlayer() == auctionedItem.getBidder()) {
                // Finish bidding
                assignItem(auctionedItem.getBidder(), auctionedItem,
                        auctionedItem.getBid(), 0);
                currentStep.set(SELECT_STEP);
                setNextSelectingPlayer();
                numPasses.set(0);
            }
            break;
        case SELECT_STEP:
            numPasses.add(1);
            if (numPasses.value() == playerManager.getNumberOfPlayers()) {
                finishRound();
            } else {
                setNextSelectingPlayer();
            }

        }

        return true;
    }

    private void setNextBiddingPlayer() {
        Player currentPlayer;
        do {
            currentPlayer = playerManager.setCurrentToNextPlayer();
        } while ( !currentAuctionItem.value().isActive(currentPlayer) );
    }

    protected void setNextSelectingPlayer() {
        playerManager.setCurrentToNextPlayerAfter(selectingPlayer.value());
    }

}
