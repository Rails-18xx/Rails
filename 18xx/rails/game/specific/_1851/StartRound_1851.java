/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1851/StartRound_1851.java,v 1.9 2009/10/07 21:03:36 evos Exp $ */
package rails.game.specific._1851;

import java.util.List;

import rails.game.*;
import rails.game.action.*;
import rails.util.LocalText;

/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_1851 extends StartRound {

    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1851(GameManagerI gameManager) {
        super(gameManager);
        hasBidding = false;
    }

    /**
     * Start the 1835-style start round.
     *
     * @param startPacket The startpacket to be sold in this start round.
     */
    @Override
    public void start() {
        super.start();

        if (!setPossibleActions()) {
            /*
             * If nobody can do anything, keep executing Operating and Start
             * rounds until someone has got enough money to buy one of the
             * remaining items. The game mechanism ensures that this will
             * ultimately be possible.
             */
            finishRound();
        }

    }

    /**
     * Get a list of items that may be bought immediately. <p> In an 1835-style
     * auction this method will usually return several items.
     *
     * @return An array of start items that can be bought.
     */

    @Override
    public boolean setPossibleActions() {

        StartItemAction action;
        List<StartItem> startItems = startPacket.getItems();

        possibleActions.clear();

        for (StartItem item : startItems) {
            if (!item.isSold()) {
                item.setStatus(StartItem.BUYABLE);
                possibleActions.add(action =
                        new BuyStartItem(item, item.getBasePrice(), false));
                log.debug(getCurrentPlayer().getName() + " may: "
                          + action.toString());
            }

        }

        /* Pass is not allowed */

        return true;
    }

    @Override
    public List<StartItem> getStartItems() {
        Player currentPlayer = getCurrentPlayer();
        int cashToSpend = currentPlayer.getCash();
        List<StartItem> startItems = startPacket.getItems();

        for (StartItem item : startItems) {
            if (item.isSold()) {
                item.setStatus(StartItem.SOLD);
            } else if (item.getBasePrice() > cashToSpend) {
                item.setStatus(StartItem.UNAVAILABLE);
            } else {
                item.setStatus(StartItem.BUYABLE);
            }
        }
        return startItems;
    }

    /*----- MoveSet methods -----*/

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
        log.error("Unexcpected pass");
        return false;
    }

    @Override
    public String getHelp() {
        return "1851 Start Round help text";
    }

}
