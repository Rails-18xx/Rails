package rails.game.specific._1851;

import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;

public class StartRound_1851 extends StartRound {

    /**
     * Constructed via Configure
     */
    public StartRound_1851(GameManager parent, String id) {
        super(parent, id);
        hasBidding = false;
    }

    /**
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
                log.debug(getCurrentPlayer().getId() + " may: "
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
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    public boolean pass(NullAction action, String playerName) {
        log.error("Unexcpected pass");
        return false;
    }

    @Override
    public String getHelp() {
        return "1851 Start Round help text";
    }

}
