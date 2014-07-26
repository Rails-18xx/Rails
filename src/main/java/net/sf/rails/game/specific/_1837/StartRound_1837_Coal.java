package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.state.IntegerState;
import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;

/**
 * Implements an 1837-style startpacket sale.
 */
public class StartRound_1837_Coal extends StartRound {
    protected final int bidIncrement;

    protected IntegerState numRoundsPassed = IntegerState.create(this,"StartRoundRoundsPassed");
    
    /**
     * Constructor, only to be used in dynamic instantiation.
     */
    public StartRound_1837_Coal(GameManager gameManager, String id) {
        super(gameManager, id, false, true, true);
        bidIncrement = startPacket.getModulus();
    }

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
            //gameManager.nextRound(this);
            finishRound();
        }

    }

    @Override
    public boolean setPossibleActions() {

        List<StartItem> startItems =  startPacket.getItems();
        List<StartItem> buyableItems = new ArrayList<StartItem>();
        int row;
        int column;
        boolean buyable;
        int minRow = 0;
        boolean[][] soldStartItems = new boolean [3][6];
                
        /*
         * First, mark which items are buyable. Once buyable, they always remain
         * so until bought, so there is no need to check if an item is still
         * buyable.
         */
        if ((!startPacket.areAllSold()) ){
                 for (StartItem item : startItems) {
                    buyable = false;
        
                    column= item.getColumn();
                    row = item.getRow();
                    
                    if (item.isSold()) {
                        // Already sold: skip but set watermarks
        
                        
                        if (column ==1) {
                            soldStartItems[0][row-1] = true;
                        } else {
                            if (column ==2) {
                                soldStartItems[1][row-1] = true;
                            } else {
                                soldStartItems[2][row-1] = true;
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
                            // Allow the first item in the next row of a column where the items in higher 
                            //rows have been bought.
                            if (soldStartItems[column-1][row-2] == true) {                    
                            buyable = true;
                                }
                            }
                        }
                        if (buyable) {
                        item.setStatus(StartItem.BUYABLE);
                        buyableItems.add(item);
                        }
                    } //startItems
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
            if (currentPlayer == startPlayer) ReportBuffer.add(this,"");

            int cashToSpend = currentPlayer.getCash();

            for (StartItem item : buyableItems) {
                 if (item.getBasePrice() <= cashToSpend) {
                    /* Player does have the cash */
                    possibleActions.add(new BuyStartItem(item,
                            item.getBasePrice(), false));
                }
            }

            // setNextPlayer();
      }

        /* Pass is always allowed */
        possibleActions.add(new NullAction(NullAction.Mode.PASS));

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
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

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
            // The next open top row papers in either column will be reduced by price 
            // TBD
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            for (StartItem item : startPacket.getItems()) {
                if ((item.getStatus() == 2) && (item.getBasePrice() != 0)) {
                    item.reduceBasePriceBy(10);
                    ReportBuffer.add(this, LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                                    item.getName(),
                                  Bank.format(this, item.getBasePrice()) ));
                }
            }
                   
            numPasses.set(0);
            if (startPacket.getFirstUnsoldItem().getBasePrice() == 0) {
                assignItem(playerManager.getCurrentPlayer(),
                        startPacket.getFirstUnsoldItem(), 0, 0);
                getRoot().getPlayerManager().setPriorityPlayerToNext();
                } else {
                //BR: If the first item's price is reduced, but not to 0, 
                //    we still need to advance to the next player
                playerManager.setCurrentToNextPlayer();
           
                }
            numRoundsPassed.add(1);
            
        }  else {
            playerManager.setCurrentToNextPlayer();
        }

        return true;
    }
    
    

    /* (non-Javadoc)
     * @see rails.game.StartRound#buy(java.lang.String, rails.game.action.BuyStartItem)
     */
    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        // TODO: If the player buys a price reduced paper the other price reduced papers need to be set back to the 
        // base price
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
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }

        assignItem(player, item, price, sharePrice);

        // Set priority (only if the item was not auctioned)
        // ASSUMPTION: getting an item in auction mode never changes priority
        if (lastBid == 0) {
            getRoot().getPlayerManager().setPriorityPlayerToNext();
        }
        playerManager.setCurrentToNextPlayer();
        resetStartPacketPrices(numRoundsPassed.value());
        numPasses.set(0);
        numRoundsPassed.set(0);
       
        if (startPacket.areAllSold()) {
            finishRound();
        }
        return true;

    }


    private void resetStartPacketPrices(int i) {
        List<StartItem> startItems =  startPacket.getItems();
        for(StartItem item: startItems) {
            if ((!item.isSold())&& (item.getStatus() == StartItem.BUYABLE)){
            item.reduceBasePriceBy(-(i*10));
            }
        }
        
    }

    @Override
    public String getHelp() {
        return "1837 Start Round help text";
    }


}
