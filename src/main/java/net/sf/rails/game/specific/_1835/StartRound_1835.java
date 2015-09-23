package net.sf.rails.game.specific._1835;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.state.IntegerState;


/**
 * Implements an 1835-style startpacket sale.
 */

// FIXME: Check if this still works, as this is now done via reverse
public class StartRound_1835 extends StartRound {

    /* To control the player sequence in the Clemens and Snake variants */
    private IntegerState turn = IntegerState.create(this, "TurnNumber", 0);

    /* Additional variants */
    public static final String CLEMENS_VARIANT = "Clemens";
    public static final String SNAKE_VARIANT = "Snake";

    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1835(GameManager gameManager, String id) {
        super(gameManager, id, false, true, true);
        // no bidding involved
    }

    @Override
    public void start() {

        if (variant.equalsIgnoreCase(CLEMENS_VARIANT) && gameManager.getStartRoundNumber() == 1) {
            // reverse order at the start (only in the first start Round)
            playerManager.reversePlayerOrder(true);
            // set priority to last player
            Player lastPlayer = playerManager.getNextPlayerAfter(playerManager.getPriorityPlayer());
            playerManager.setPriorityPlayer(lastPlayer);
        }
        // then continue with standard start round
        super.start();

        if (!setPossibleActions()) {
            /*
             * If nobody can do anything, keep executing Operating and Start
             * rounds until someone has got enough money to buy one of the
             * remaining items. The game mechanism ensures that this will
             * ultimately be possible.
             */
            //gameManager.nextRound(this);
            finishRound();
        }

    }

    @Override
    public boolean setPossibleActions() {

        List<StartItem> startItems = startPacket.getItems();
        List<StartItem> buyableItems = new ArrayList<StartItem>();
        int row;
        boolean buyable;
        int items = 0;
        int minRow = 0;

        /*
         * First, mark which items are buyable. Once buyable, they always remain
         * so until bought, so there is no need to check is an item is still
         * buyable.
         */
        for (StartItem item : startItems) {
            buyable = false;

            if (item.isSold()) {
                // Already sold: skip
            } else if (variant.equalsIgnoreCase(CLEMENS_VARIANT)) {
                buyable = true;
            } else {
                row = item.getRow();
                if (minRow == 0) minRow = row;
                if (row == minRow) {
                    // Allow all items in the top row.
                    buyable = true;
                } else if (row == minRow + 1 && items == 1) {
                    // Allow the first item in the next row if the
                    // top row has only one item.
                    buyable = true;
                }
            }
            if (buyable) {
                items++;
                item.setStatus(StartItem.BUYABLE);
                buyableItems.add(item);
            }
        }
        possibleActions.clear();

        /*
         * Repeat until we have found a player with enough money to buy some
         * item
         */
        while (possibleActions.isEmpty()) {

            Player currentPlayer = playerManager.getCurrentPlayer();
            if (currentPlayer == startPlayer) ReportBuffer.add(this, "");

            int cashToSpend = currentPlayer.getCash();

            for (StartItem item : buyableItems) {

                if (item.getBasePrice() <= cashToSpend) {
                    /* Player does have the cash */
                    possibleActions.add(new BuyStartItem(item,
                            item.getBasePrice(), false));
                }
            }

            if (possibleActions.isEmpty()) {
                String message =
                    LocalText.getText("CannotBuyAnything",
                            currentPlayer.getId());
                ReportBuffer.add(this, message);
                numPasses.add(1);
                if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
                    // All players have passed.
                    gameManager.reportAllPlayersPassed();
                    /*
                     * No-one has enough cash left to buy anything, so close the
                     * Start Round.
                     */
                    numPasses.set(0);
                    finishRound();
                    gameManager.getCurrentRound().setPossibleActions();

                    // This code may be called recursively.
                    // Jump out as soon as we have something to do
                    if (!possibleActions.isEmpty()) break;

                    return false;
                }
                checkPlayerOrder();
                playerManager.setCurrentToNextPlayer();
            }
        }

        /* Pass is always allowed */
        possibleActions.add(new NullAction(NullAction.Mode.PASS));

        return true;
    }

    @Override
    public boolean buy(String playerName, BuyStartItem boughtItem) {
        boolean result = super.buy(playerName, boughtItem);
        if (result) {
            checkPlayerOrder();
        }
        return result;
    }
    
    @Override
    public boolean bid(String playerName, BidStartItem item) {
        // is not allowed in 1835
        return false;
    }
    
        
    @Override
    public boolean process(PossibleAction action) {
        // nothing else to do in 1835, just a reminder
        boolean result = super.process(action);
        return result;
    }

    /**
     * Process a player's pass.
     *
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    public boolean pass(NullAction action, String playerName) {

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

        if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
            // All players have passed.
            gameManager.reportAllPlayersPassed();
            numPasses.set(0);
            //gameManager.nextRound(this);
            finishRound();
        } else {
            checkPlayerOrder();
            playerManager.setCurrentToNextPlayer();
        }

        return true;
    }

    // for some variants the player has to be changed in-between 
    private void checkPlayerOrder() {
        if (gameManager.getStartRoundNumber() == 1) {
            /*
             * Some variants have a reversed player order in the first or second
             * cycle of the first round (a cycle spans one turn of all players).
             * In such a case we need to keep track of the number of player
             * turns.
             */
            turn.add(1);
            // check if the next player would start a new cycle
            int cycleNumber = (turn.value()) / playerManager.getNumberOfPlayers();
            int playerNumber = (turn.value()) % playerManager.getNumberOfPlayers();
            log.debug("1835 variant = " + variant + ", turn = " + turn.value() + ", cycleNumber = " + cycleNumber + ", playerNumber = " + playerNumber);
            if (variant.equalsIgnoreCase(CLEMENS_VARIANT)) {
                /*  */
                if (cycleNumber == 1 && playerNumber == 0) {
                    // restore player Order
                    playerManager.reversePlayerOrder(false);
                    // move one player ahead as we were one player ahead
                    playerManager.setCurrentToNextPlayer();
                }
            } else if (variant.equalsIgnoreCase(SNAKE_VARIANT)) {
                /* Reverse order in the second cycle (this is cycleNr = 1) */
                if (cycleNumber == 1 && playerNumber == 0) {
                    playerManager.reversePlayerOrder(true);
                    // set priority to last player
                    Player lastPlayer = playerManager.getNextPlayerAfter(playerManager.getCurrentPlayer());
                    playerManager.setCurrentPlayer(lastPlayer);
                } else if (cycleNumber == 2 && playerNumber == 0) {
                    // restore player Order
                    playerManager.reversePlayerOrder(false);
                    // move one player ahead as we were one player ahead
                    playerManager.setCurrentToNextPlayer();
                }
            }
        }
    }

}
