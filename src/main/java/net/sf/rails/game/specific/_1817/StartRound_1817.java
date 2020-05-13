package net.sf.rails.game.specific._1817;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;

public class StartRound_1817 extends StartRound {

    public final static int SELECT_STEP = 0;
    public final static int OPEN_STEP = 1;
    public final static int BID_STEP = 2;
    
    private final IntegerState currentStep = IntegerState.create(this, "currentStep", SELECT_STEP);
    private final GenericState<Player> selectingPlayer = new GenericState(this, "selectingPlayer");
    private final GenericState<StartItem> currentAuctionItem = new GenericState(this, "currentAuctionItem");

    /**
     * Seedmoney
     */
    private final IntegerState SeedMoney = IntegerState.create(this, "seedmoney", 200);
    
    /**
     * Constructed via Configure
     */
    public StartRound_1817(GameManager parent, String id) {
        super(parent, id, true, false, true);
        // bidding, but no base prices
    }

    @Override
    public void start() {
        super.start();

        currentStep.set(SELECT_STEP);

        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();
        
        Player currentPlayer = playerManager.getCurrentPlayer();
        int bidMinimum = 0;
        switch (currentStep.value()) {
        case SELECT_STEP:
            // In the selection step, all not yet sold items are selectable.
            // The current player MUST select an item,
            // and may then bid for it or pass.

            selectingPlayer.set(currentPlayer);

            for (StartItem item : itemsToSell.view()) {
            	
                if (!item.isSold()) {
                	bidMinimum= ((item.getBasePrice()-SeedMoney.value())>0) ?(item.getBasePrice()-SeedMoney.value()) :0;
                    item.setStatus(StartItem.SELECTABLE);
                    item.setMinimumBid(bidMinimum);
                    BidStartItem possibleAction =
                            new BidStartItem(item, bidMinimum,
                                    startPacket.getModulus(), false, true);
                    possibleActions.add(possibleAction);
                }
            }
            break;
        case OPEN_STEP:
        case BID_STEP:
            StartItem item = (StartItem) currentAuctionItem.value();
            // only offer if enough money and below maximum bid
            if (item.getMinimumBid() <= (currentPlayer.getFreeCash()+SeedMoney.value()) && item.getBid() < item.getBasePrice()){
            	
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




    /**
     * The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param bidItem The item on which the bid is placed.
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        StartItem auctionedItem = (StartItem) currentAuctionItem.value();
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
                // Player may not exceed face value
                if (bidAmount > item.getBasePrice()) {
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
/*        case OPEN_STEP:
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
            break;*/

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

        switch (currentStep.value()) {
/*        case OPEN_STEP:
        case BUY_STEP:
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
            break;*/

        case BID_STEP:
            
            auctionedItem.setPass(playerManager.getCurrentPlayer());

            // We are done if the next still bidding player
            // is equal to the current highest bidder.
            setNextBiddingPlayer();
            if (playerManager.getCurrentPlayer() == auctionedItem.getBidder()) {
                
            	// Finish bidding
            	SeedMoney.set(SeedMoney.value()-(auctionedItem.getBasePrice()-auctionedItem.getBid()));
                assignItem(auctionedItem.getBidder(), auctionedItem,
                		auctionedItem.getBid()   , 0);
                currentStep.set(SELECT_STEP);
                setNextSelectingPlayer();
            }
        }

        return true;
    }

    private void setNextBiddingPlayer() {
        
        Player currentPlayer;
        do {
            currentPlayer = playerManager.setCurrentToNextPlayer();
        } while (((StartItem) currentAuctionItem.value()).isActive(currentPlayer) == false);
    }

    private void setNextSelectingPlayer() {
        playerManager.setCurrentToNextPlayerAfter(selectingPlayer.value());
    }

}
