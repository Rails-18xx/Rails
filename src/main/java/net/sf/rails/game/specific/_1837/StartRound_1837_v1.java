package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.IntegerState;
import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
//import rails.game.specific._1837.SetHomeHexLocation;

/**
 * Implements an 1837-style startpacket sale.
 */
public class StartRound_1837_v1 extends StartRound {
    private static final Logger log = LoggerFactory.getLogger(StartRound_1837_v1.class);

    protected final int bidIncrement;

    protected IntegerState numRoundsPassed = IntegerState.create(this,
            "StartRoundRoundsPassed");
    protected HashMap<StartItem, Integer> originalBasePrice = new HashMap<>();


    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1837_v1(GameManager gameManager, String id) {
        super(gameManager, id, false, true, true);
        bidIncrement = startPacket.getModulus();
        this.setStartRoundName("Initial Minor StartRound (Coal and Privates including Suedbahn)");
    }

    @Override
    public void start() {


        for (StartItem item : startPacket.getItems()) {
            // New: we only include items that have not yet been sold
            // at the start of the current StartRound
            if (!item.isSold()) {
                itemsToSell.add(item);
                originalBasePrice.put (item, item.getBasePrice());
            }
        }
        numPasses.set(0);

        // init current with priority player
        startPlayer = playerManager.setCurrentToPriorityPlayer();

        ReportBuffer.add(this, LocalText.getText("StartOfStartRound",getStartRoundName()));
        ReportBuffer.add(this, LocalText.getText("HasPriority",
                startPlayer.getId()));

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

    @Override
    public boolean setPossibleActions() {

        List<StartItem> startItems = startPacket.getItems();
        List<StartItem> buyableItems = new ArrayList<StartItem>();
        int row;
        int column;
        boolean buyable;
        int minRow = 0;
        boolean[][] soldStartItems = new boolean[3][6];

        if ((!startPacket.areAllSold())) {
            for (StartItem item : startItems) {
                buyable = false;

                column = item.getColumn();
                row = item.getRow();

                if (item.isSold()) {
                    // Already sold: skip but set watermarks

                    if (column == 1) {
                        soldStartItems[0][row - 1] = true;
                    } else {
                        if (column == 2) {
                            soldStartItems[1][row - 1] = true;
                        } else {
                            soldStartItems[2][row - 1] = true;
                        }
                    }

                } else {
                    if (minRow == 0) {
                        minRow = row;
                    }
                    if (row == minRow) {
                        // Allow all items in the top row.
                        buyable = true;
                    } else {
                        // Allow the first item in the next row of a column
                        // where the items in higher
                        // rows have been bought.
                        if (soldStartItems[column - 1][row - 2] == true) {
                            buyable = true;
                        }
                    }
                }
                if (buyable) {
                    item.setStatus(StartItem.BUYABLE);
                    buyableItems.add(item);
                }
            } // startItems
            possibleActions.clear();
        } else { // Are all Sold
            possibleActions.clear();
            return true;
        }

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

            } /* Pass is always allowed */
            // No its not if there is a startItem with a price of zero the player has to buy that.
            for (StartItem item : buyableItems) {
                if (item.getBasePrice() == 0) {
                    return true;
                }
            }
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));

        }

        return true;
    }

    /*----- moveStack methods -----*/

    @Override
    public boolean bid(String playerName, BidStartItem item) {

        DisplayBuffer.add(this, LocalText.getText("InvalidAction"));
        return false;
    }

    /**
     * Process a player's pass.
     *
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg =
                        LocalText.getText("WrongPlayer", playerName,
                                player.getId());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this,
                    LocalText.getText("InvalidPass", playerName, errMsg));
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

        numPasses.add(1);

        if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
            // All players have passed.
            // The next open top row papers in either column will be reduced by
            // price
            // TBD
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            for (StartItem item : startPacket.getItems()) {
                if ((item.getStatus() == 2) && (item.getBasePrice() != 0)) {
                    if (item.getBasePrice() >=10) {
                        item.reduceBasePriceBy(10);
                    } else { //Assumption only 5 G remain
                        item.reduceBasePriceBy(5);
                    }
                    ReportBuffer.add(
                            this,
                            LocalText.getText("ITEM_PRICE_REDUCED",
                                    item.getId(),
                                    Bank.format(this, item.getBasePrice())));
                }
            }

            numPasses.set(0);
            numRoundsPassed.add(1);
            playerManager.setCurrentToNextPlayer();
       } else {
            playerManager.setCurrentToNextPlayer();
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see rails.game.StartRound#buy(java.lang.String,
     * rails.game.action.BuyStartItem)
     */
    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {

        StartItem item = boughtItem.getStartItem();
        int lastBid = item.getBid();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int price = 0;
        int sharePrice = 0;

        while (true) {
            if (item.getStatus() != StartItem.BUYABLE) {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            price = item.getBasePrice();

            if (player.getFreeCash() < price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName, item.getId(), errMsg));
            return false;
        }

        assignItem(player, item, price, sharePrice);

        // Set priority (only if the item was not auctioned)
        if (lastBid == 0) {
            getRoot().getPlayerManager().setPriorityPlayerToNext();
        }
        playerManager.setCurrentToNextPlayer();
        resetStartPacketPrices(numRoundsPassed.value());
        numPasses.set(0);
        numRoundsPassed.set(0);

        if (item.getId().equals("KB")) {
            return true;

        }

        return true;

    }

    private void resetStartPacketPrices(int i) {
        List<StartItem> startItems = startPacket.getItems();
        for (StartItem item : startItems) {
            if ((!item.isSold()) && (item.getStatus() == StartItem.BUYABLE)) {
                item.reduceBasePriceBy(-(originalBasePrice.get(item) - item.getBasePrice()));
                //item.reduceBasePriceBy(-(i * 10));// Attention there is at least one certificate that has a price not based on 10G.
             //if (item.getId() =="AB") {  <<<< not smart! And not needed too.
                 //item.reduceBasePriceBy(5);
             //}
            }
        }

    }


    @Override
    public boolean process(PossibleAction action) {
        boolean result = false;

        log.debug("Processing action {}", action);

        if (action instanceof NullAction
            && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            String playerName = action.getPlayerName();
            NullAction nullAction = (NullAction) action;
            result = pass(nullAction, playerName);

        } else if (action instanceof StartItemAction) {

            StartItemAction startItemAction = (StartItemAction) action;
            String playerName = action.getPlayerName();

            log.debug("Item details: {}", startItemAction);

            if (startItemAction instanceof BuyStartItem) {

                BuyStartItem buyAction = (BuyStartItem) startItemAction;
                result = buy(playerName, buyAction);
            } else {

                DisplayBuffer.add(
                        this,
                        LocalText.getText("UnexpectedAction", action.toString()));
            }
        }
        if ((startPacket.areAllSold())) {
                possibleActions.clear();
                finishRound();
            }
        return result;
    }

}
