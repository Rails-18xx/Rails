package rails.game.specific._1825;

import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.ReportBuffer;
import rails.game.*;
import rails.game.action.*;

public class StartRound_1825 extends StartRound {

    /**
     * Constructed via Configure
     */
    public StartRound_1825(GameManager parent, String id) {
        super(parent, id);
        hasBidding = false;
    }

    /**
     * Start the 1825-style start round.
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
     * Get a list
     *
     * @return An array of start items that can be bought.
     */

    @Override
    public boolean setPossibleActions() {

        StartItemAction action;
        List<StartItem> startItems = startPacket.getItems();
        boolean itemAvailable = false;
        int soldShares = 0;
        possibleActions.clear();
        
        for (StartItem item : startItems) {
            //Do we already have an item available for sale?
            if (itemAvailable == false){
                //If not, check whether this has already been sold
                if (!item.isSold()){
                    item.setStatus(StartItem.BUYABLE);
                    possibleActions.add(action =   
                        new BuyStartItem(item, item.getBasePrice(), false));
                    log.debug(getCurrentPlayer().getId() + " may: "
                            + action.toString());
                    //Found one, no need to find any others
                    itemAvailable = true;
                }
            }
        }
        //Does everyone have at least one private?
        //If so, then we're officially into the first share round so passing is allowed
        for (StartItem item : startItems) {
            if (item.isSold()){
                soldShares++;
            }
            if (soldShares == playerManager.getPlayers().size()){
                //Enable passing
                possibleActions.add(new NullAction(NullAction.PASS));
            }
        }
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

    @Override
    public boolean bid(String playerName, BidStartItem item) {

        DisplayBuffer.add(this, LocalText.getText("InvalidAction"));
        return false;
    }

    /**
     * Process a player's pass.
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    public boolean pass(NullAction action, String playerName) {
        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));
        numPasses.add(1);
        if (numPasses.value() >= numPlayers) {
            //Everyone has passed
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            numPasses.set(0);
            finishRound();
        }
        setNextPlayer();
        return true;
    }

    @Override
    public String getHelp() {
        return "1825 Start Round help text";
    }

}
