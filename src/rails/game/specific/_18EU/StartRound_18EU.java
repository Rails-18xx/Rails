package rails.game.specific._18EU;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.state.IntegerState;
import rails.game.state.GenericState;

public class StartRound_18EU extends StartRound {

    public final static int SELECT_STEP = 0;
    public final static int OPEN_STEP = 1;
    public final static int BUY_STEP = 2;
    public final static int BID_STEP = 3;

    private final IntegerState currentStep = IntegerState.create(this, "currentStep", SELECT_STEP);
    private final GenericState<Player> selectingPlayer = GenericState.create(this, "selectingPlayer");
    private final IntegerState currentBuyPrice = IntegerState.create(this, "currentBuyPrice");
    private final GenericState<StartItem> currentAuctionItem = GenericState.create(this, "currentAuctionItem");

    /**
     * Constructed via Configure
     */
    public StartRound_18EU(GameManager parent, String id) {
        super(parent, id);
        hasBidding = true;
        hasBasePrices = false;
    }

    /**
     * Start the 18EU-style start round.
     *
     * @param startPacket The startpacket to be sold in this start round.
     */
    @Override
    public void start() {
        super.start();

        setStep(SELECT_STEP);

        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();

        // Refresh player, may have been reset by Undo/Redo
        currentPlayer = getCurrentPlayer();

        switch (getStep()) {
        case SELECT_STEP:
            // In the selection step, all not yet sold items are selectable.
            // The current player MUST select an item,
            // and may then bid for it or pass.

            selectingPlayer.set(getCurrentPlayer());
            currentBuyPrice.set(100);

            for (StartItem item : itemsToSell.view()) {
                if (!item.isSold()) {
                    item.setStatus(StartItem.SELECTABLE);
                    item.setMinimumBid(item.getBasePrice());
                    BidStartItem possibleAction =
                            new BidStartItem(item, item.getBasePrice(),
                                    startPacket.getModulus(), false, true);
                    possibleActions.add(possibleAction);
                }
            }
            break;
        case BUY_STEP:
            // only offer buy if enough money
            if (currentBuyPrice.value() <= currentPlayer.getFreeCash()) {
                possibleActions.add(new BuyStartItem(
                        (StartItem) currentAuctionItem.value(),
                        currentBuyPrice.value(), true));
            }
            possibleActions.add(new NullAction(NullAction.PASS));
            break;
        case OPEN_STEP:
        case BID_STEP:
            StartItem item = (StartItem) currentAuctionItem.value();
            // only offer if enough money
            if (item.getMinimumBid() <= currentPlayer.getFreeCash()) {
                BidStartItem possibleAction =
                    new BidStartItem(item, item.getMinimumBid(),
                            startPacket.getModulus(), true);
                possibleActions.add(possibleAction);
            }
            if (getStep() == OPEN_STEP) {
                possibleActions.add(new NullAction(NullAction.PASS).setLabel("DeclineToBid"));
            } else {
                possibleActions.add(new NullAction(NullAction.PASS));
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
        Player player = getCurrentPlayer();
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
            DisplayBuffer.add(LocalText.getText("CantBuyItem",
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }

        

        assignItem(player, item, price, 0);
        ((PublicCertificate) item.getPrimary()).getCompany().start();
        setNextSelectingPlayer();
        setStep(SELECT_STEP);

        return true;

    }

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
        StartItem auctionedItem = (StartItem) currentAuctionItem.value();
        String errMsg = null;
        Player player = getCurrentPlayer();
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
                if ((getStep() == OPEN_STEP || getStep() == BID_STEP)
                    && !item.equals(auctionedItem)) {
                    errMsg =
                            LocalText.getText("WrongStartItem",
                                    item.getName(),
                                    auctionedItem.getName() );
                    break;
                }

                // Bid must be at least the minimum bid
                if (bidAmount < item.getMinimumBid()) {
                    errMsg =
                            LocalText.getText("BidTooLow",
                                    Currency.format(this, item.getMinimumBid()));
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
                                    Currency.format(this, bidAmount));
                    break;
                }
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

        

        if (getStep() == SELECT_STEP) {

            currentAuctionItem.set(item);
            item.setStatus(StartItem.AUCTIONED);
            for (StartItem item2 : itemsToSell.view()) {
                if (item2 != item && !item2.isSold()) {
                    item2.setStatus(StartItem.UNAVAILABLE);
                }
            }
            if (bidAmount == -1) {
                setStep(OPEN_STEP);
            }

            ReportBuffer.add(this, " ");
            ReportBuffer.add(this, LocalText.getText("SelectForAuctioning",
                    playerName,
                    item.getName() ));
        }

        if (bidAmount > 0) {
            item.setBid(bidAmount, player);
            item.setMinimumBid(bidAmount + 5);
            setStep(BID_STEP);

            ReportBuffer.add(this, LocalText.getText("BID_ITEM",
                    playerName,
                    Currency.format(this, bidAmount),
                    item.getName() ));
        }

        switch (getStep()) {
        case OPEN_STEP:
        case BUY_STEP:
            setNextPlayer();
            if (currentPlayer == selectingPlayer.value()) {
                // All have passed, now lower the buy price
                currentBuyPrice.add(-10);
                setStep(BUY_STEP);
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

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

        

        StartItem auctionedItem = (StartItem) currentAuctionItem.value();

        switch (getStep()) {
        case OPEN_STEP:
        case BUY_STEP:
            setNextPlayer();
            if (currentPlayer == selectingPlayer.value()) {
                // All have passed, now lower the buy price
                currentBuyPrice.add(-10);
                auctionedItem.setMinimumBid(currentBuyPrice.value());
                ReportBuffer.add(this, LocalText.getText("ITEM_PRICE_REDUCED",
                        auctionedItem.getName(),
                        Currency.format(this, currentBuyPrice.value()) ));
                setStep(BUY_STEP);

                if (currentBuyPrice.value() == 0) {
                    // Forced buy
                    // Trick to make the zero buy price visible
                    auctionedItem.setBid(0, currentPlayer);

                    assignItem(currentPlayer, auctionedItem, 0, 0);
                    setStep(SELECT_STEP);
                    setNextSelectingPlayer();
                }
            }
            break;

        case BID_STEP:

            auctionedItem.setBid(-2, currentPlayer);

            // We are done if the next still bidding player
            // is equal to the current highest bidder.
            setNextBiddingPlayer();
            if (currentPlayer == auctionedItem.getBidder()) {
                // Finish bidding
                assignItem(auctionedItem.getBidder(), auctionedItem,
                        auctionedItem.getBid(), 0);
                setStep(SELECT_STEP);
                setNextSelectingPlayer();
            }
        }

        return true;
    }

    private void setNextBiddingPlayer() {

        do {
            setNextPlayer();
        } while (((StartItem) currentAuctionItem.value()).getBid(currentPlayer) < 0);
    }

    private void setNextSelectingPlayer() {
        setPlayer((Player) selectingPlayer.value());
        setNextPlayer();
    }

    public int getStep() {
        return currentStep.value();
    }

    public void setStep(int step) {
        if (step != currentStep.value()) {
            currentStep.set(step);
        }
    }

    @Override
    public String getHelp() {
        return "1835 Start Round help text";
    }

}
