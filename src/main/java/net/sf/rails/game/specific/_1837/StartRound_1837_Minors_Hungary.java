package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;

public class StartRound_1837_Minors_Hungary extends StartRound {

    public StartRound_1837_Minors_Hungary(GameManager gameManager, String id) {
        super(gameManager, id, false, true, true);
        this.setStartRoundName("Minor Hungary StartRound");
    }
 
    
    /* (non-Javadoc)
     * @see net.sf.rails.game.specific._1837.StartRound_1837_Coal#start()
     */
    @Override
    public void start() {

        for (StartItem item : startPacket.getItems()) {
            // New: we only include items that have not yet been sold
            // at the start of the current StartRound
            if (!item.isSold()) {
                itemsToSell.add(item);
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
   
    
    /* (non-Javadoc)
     * @see net.sf.rails.game.specific._1837.StartRound_1837_Coal#setPossibleActions()
     */
    @Override
    public boolean setPossibleActions() {

        List<StartItem> startItems =  startPacket.getItems();
        List<StartItem> buyableItems = new ArrayList<StartItem>();

        if ((!startPacket.areAllSold()) ){
            for (StartItem item : startItems) {
                if (!item.isSold()) {
                    //25 % papers of U1 and U3 can only be bought if the President has been sold.
                    if (item.getName().equals("U1W") && (!(startItems.get(0).isSold()))) continue;
                    if (item.getName().equals("U3C") && (!(startItems.get(3).isSold()))) continue;
                   item.setStatus(StartItem.BUYABLE);
                   buyableItems.add(item);
                   }
            }
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
            }  /* Pass is always allowed */
            possibleActions.add(new NullAction(NullAction.Mode.PASS));
                        
        }

        return true;
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
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            numPasses.set(0);
            finishRound();
        } else {
            playerManager.setCurrentToNextPlayer();
        }

        return true;
    }


    @Override
    protected boolean bid(String playerName, BidStartItem startItem) {
        // TODO Auto-generated method stub
        return false;
    }
}
        
