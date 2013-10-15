/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StartRound_1835.java,v 1.26 2010/03/30 21:59:03 evos Exp $ */
package rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.state.IntegerState;

/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_1837 extends StartRound {


    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1837(GameManagerI gameManager) {
        super(gameManager);
        hasBidding = false;
    }

    /**
     * Start the 1835-style start round.
     *
     * @param startPacket The startpacket to be sold in this start round.
     */
    @Override
    public void start(StartPacket startPacket) {
        super.start(startPacket);

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
        int column;
        boolean buyable;
        int items = 0;
        int minRow = 0;

        /*
         * First, mark which items are buyable. Once buyable, they always remain
         * so until bought, so there is no need to check if an item is still
         * buyable.
         */
        for (StartItem item : startItems) {
            buyable = false;

            if (item.isSold()) {
                // Already sold: skip but set watermarks

           } else {
                column= item.getColumn();
                row = item.getRow();
                if (minRow == 0) minRow = row;
                if (row == minRow) {
                    // Allow all items in the top row.
                    buyable = true;
                } else {
                    // Allow the first item in the next row of a column where the items in lower 
                    //rows have been bought.
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

            Player currentPlayer = getCurrentPlayer();
            if (currentPlayer == startPlayer) ReportBuffer.add("");

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
                            currentPlayer.getName());
                ReportBuffer.add(message);
                //DisplayBuffer.add(message);
                numPasses.add(1);
                if (numPasses.intValue() >= numPlayers) {
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
                setNextPlayer();
            }
        }

        /* Pass is always allowed */
        possibleActions.add(new NullAction(NullAction.PASS));

        return true;
    }

    /*----- moveStack methods -----*/

    @Override
    public boolean bid(String playerName, BidStartItem item) {

        DisplayBuffer.add(LocalText.getText("InvalidAction"));
        return false;
    }


    /**
     * Process a player's pass.
     *
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    public boolean pass(String playerName) {

        String errMsg = null;
        Player player = getCurrentPlayer();

        while (true) {

            // Check player
            if (!playerName.equals(player.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getName());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidPass",
                    playerName,
                    errMsg ));
            return false;
        }

        ReportBuffer.add(LocalText.getText("PASSES", playerName));

        moveStack.start(false);

        numPasses.add(1);

        if (numPasses.intValue() >= numPlayers) {
            // All players have passed.
            ReportBuffer.add(LocalText.getText("ALL_PASSED"));
            numPasses.set(0);
            //gameManager.nextRound(this);
            finishRound();
        } else {
            setNextPlayer();
        }

        return true;
    }

    @Override
    public String getHelp() {
        return "1835 Start Round help text";
    }

}
