package net.sf.rails.game;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;


public class PlayerManager extends RailsManager implements Configurable {

    private static Logger log =
            LoggerFactory.getLogger(PlayerManager.class);

    // static data
    private Map<String, Player> playerNames; // static, but set later in setPlayers()
    
    // configure data
    private int maxPlayers;
    private int minPlayers;
    private final Map<Integer, Integer> playerStartCash = 
            Maps.newHashMap();
    private final Map<Integer, Integer> playerCertificateLimits = 
            Maps.newHashMap();
    
    // dynamic data
    private final PlayerOrderModel playerModel = new PlayerOrderModel(this, "playerModel");
    private final GenericState<Player> currentPlayer = GenericState.create(this, "currentPlayer");
    private final GenericState<Player> priorityPlayer = GenericState.create(this, "priorityPlayer");
    private final IntegerState playerCertificateLimit = IntegerState.create(this, "playerCertificateLimit");

    /**
     * nextPlayerMessages collects all messages to be displayed to the next player
     */
    private final ArrayListState<String> nextPlayerMessages = ArrayListState.create(this, "nextPlayerMessages");

    
    /**
     * Used by Configure (via reflection) only
     */
    public PlayerManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        int number, startCash, certLimit;

        List<Tag> playerTags = tag.getChildren("Players");
        minPlayers = 99;
        maxPlayers = 0;
        for (Tag playerTag : playerTags) {
            number = playerTag.getAttributeAsInteger("number");
            startCash = playerTag.getAttributeAsInteger("cash");
            playerStartCash.put(number, startCash);
            certLimit = playerTag.getAttributeAsInteger("certLimit");
            playerCertificateLimits.put(number, certLimit);

            minPlayers = Math.min(minPlayers, number);
            maxPlayers = Math.max(maxPlayers, number);
        }
    }

    // TODO: rename to initPlayers
    public void setPlayers (List<String> playerNames, Bank bank) {

        int startCash = playerStartCash.get(playerNames.size());
        
        int playerIndex = 0;
        String cashText = null;
        ImmutableMap.Builder<String, Player> playerNamesBuilder = 
                ImmutableMap.builder();
        for (String playerName : playerNames) {
            Player player = Player.create(this, playerName, playerIndex++);
            playerModel.playerOrder.add(player);
            playerNamesBuilder.put(player.getId(), player);
            cashText = Currency.fromBank(startCash, player);
            ReportBuffer.add(this, LocalText.getText("PlayerIs",
                    playerIndex,
                    player.getId() ));
        }
        this.playerNames = playerNamesBuilder.build();
        
        ReportBuffer.add(this, LocalText.getText("PlayerCash", cashText));
        ReportBuffer.add(this, LocalText.getText("BankHas", Bank.format(this, bank.getCash())));
    }
    
    public void finishConfiguration (RailsRoot root) {
        for (Player player:playerModel.playerOrder) {
            player.finishConfiguration(root);
        }
    }
    
    // sets initial priority player and certificate limits
    // TODO: rename method
    public void init() {
        priorityPlayer.set(playerModel.playerOrder.get(0));
        int startCertificates = playerCertificateLimits.get(playerModel.playerOrder.size());
        playerCertificateLimit.set(startCertificates);
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }

    public ImmutableList<Player> getPlayers() {
        return playerModel.playerOrder.view();
    }

    public Player getPlayerByName(String name) {
        return playerNames.get(name);
    }

    /**
     * @return number of players including those which are bankrupt
     */
    public int getNumberOfPlayers() {
        return playerModel.playerOrder.size();
    }
    
    int getNumberOfActivePlayers() {
        int number = 0;
        for (Player player : getPlayers()) {
            if (!player.isBankrupt()) number++;
        }
        return number;
    }

    // dynamic getter/setters
    public GenericState<Player> getCurrentPlayerModel() {
        return currentPlayer;
    }
    
    public Player getCurrentPlayer() {
        return currentPlayer.value();
    }
    
    public void setCurrentPlayer(Player player) {
        // transfer messages for the next player to the display buffer
        // TODO: refactor nextPlayerMessages inside DisplayBuffer
        if (getCurrentPlayer() != player && !nextPlayerMessages.isEmpty()) {
            DisplayBuffer.add(this, 
                    LocalText.getText("NextPlayerMessage", getCurrentPlayer().getId()));
            for (String s:nextPlayerMessages.view())
                DisplayBuffer.add(this, s);
            nextPlayerMessages.clear();
        }
        currentPlayer.set(player);
    }
    
    public Player getPriorityPlayer() {
        return priorityPlayer.value();
    }
    
    public GenericState<Player> getPriorityPlayerState() {
        return priorityPlayer;
    }
    
    public void setPriorityPlayer(Player player) {
        priorityPlayer.set(player);
    }

    public int getPlayerCertificateLimit(Player player) {
        return playerCertificateLimit.value();
    }

    public void setPlayerCertificateLimit(int newLimit) {
        playerCertificateLimit.set (newLimit);
    }

    public IntegerState getPlayerCertificateLimitModel () {
        return playerCertificateLimit;
    }
    
    /**
     * Use setCurrentPlayer instead
     */
    @Deprecated 
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        // DO NOTHING
    }
    
    public Player setCurrentToNextPlayer() {
        Player nextPlayer = getNextPlayer();
        setCurrentPlayer(nextPlayer);
        return nextPlayer;
    }
    
    public Player setCurrentToPriorityPlayer() {
        setCurrentPlayer(priorityPlayer.value());
        return priorityPlayer.value();
    }
    
    public Player setCurrentToNextPlayerAfter(Player player){
        Player nextPlayer = getNextPlayerAfter(player);
        setCurrentPlayer(nextPlayer);
        return nextPlayer;
    }
    
    public Player getNextPlayer() {
        return playerModel.getPlayerAfter(currentPlayer.value());
    }
    
    public Player getNextPlayerAfter(Player player) {
        return playerModel.getPlayerAfter(player);
    }
    

    /**
     * @boolean include the current player at the start
     * @return a list of the next (active) playerOrder after the current player
     * (including/excluding the current player at the start)
     */
    public ImmutableList<Player> getNextPlayers(boolean include) {
        return getNextPlayersAfter(currentPlayer.value(), include , false);
    }
    
    /**
     * @param boolean include the argument player at the start
     * @param boolean include the argument player at the end
     * @return a list of the next (active) playerOrder after the argument player
     * (including / excluding the argument player)
     */
    public ImmutableList<Player> getNextPlayersAfter(Player player, boolean includeAtStart, boolean includeAtEnd) {
        ImmutableList.Builder<Player> playersAfter = ImmutableList.builder();
        if (includeAtStart) {
            playersAfter.add(player);
        }
        Player nextPlayer = playerModel.getPlayerAfter(player);
        while (nextPlayer != player) {
            playersAfter.add(nextPlayer);
            nextPlayer = playerModel.getPlayerAfter(nextPlayer);
        }
        if (includeAtEnd) {
            playersAfter.add(player);
        }
        return playersAfter.build();
    }
    
    // TODO: Check if this change is valid to set only non-bankrupt playerOrder
    // to be priority playerOrder
    public Player setPriorityPlayerToNext() {
        Player priorityPlayer = getNextPlayer();
        setPriorityPlayer(priorityPlayer);
        return priorityPlayer;
    }
    
    @Deprecated
    public Player getPlayerByIndex(int index) {
        return playerModel.playerOrder.get(index % getNumberOfPlayers());
    }
    
    /**
    *
    *@param ascending Boolean to determine if the playerlist will be sorted in ascending or descending order based on their cash
    *@return Returns the player at index position 0 that is either the player with the most or least cash depending on sort order.
    */
    public Player reorderPlayersByCash (boolean ascending) {

       final boolean ascending_f = ascending;
        
       Comparator<Player> cashComparator =  new Comparator<Player>() {
           public int compare (Player p1, Player p2) {
               return ascending_f ? p1.getCash() - p2.getCash() : p2.getCash() - p1.getCash();
           }
       };

       playerModel.reorder(cashComparator);
       
       // only provide some logging
       int p = 0;
       for (Player player:playerModel.playerOrder) {
           log.debug("New player "+ String.valueOf(++p) +" is "+player.getId() +
                   " (cash="+Bank.format(this, player.getCash())+")");
       }

       return playerModel.playerOrder.get(0);
    }
    
    public void reversePlayerOrder(boolean reverse) {
        playerModel.reverse.set(reverse);
    }

    public PlayerOrderModel getPlayerOrderModel() {
       return playerModel;
    }

    public static class PlayerOrderModel extends RailsModel {

        private final ArrayListState<Player> playerOrder = ArrayListState.create(this, "playerOrder");
        private final BooleanState reverse = BooleanState.create(this, "reverse");

        private PlayerOrderModel(PlayerManager parent, String id) {
            super(parent, id);
            reverse.set(false);
        }
    
        private Player getPlayerAfter(Player player) {
            int nextIndex = playerOrder.indexOf(player);
            do {
                if (reverse.value()) {
                    nextIndex = (nextIndex - 1 + playerOrder.size()) % playerOrder.size();
                } else {
                    nextIndex = (nextIndex + 1) % playerOrder.size();
                }
            } while (playerOrder.get(nextIndex).isBankrupt());
            return playerOrder.get(nextIndex);
        }
        
        // this creates a new order based on the comparator provided
        // last tie-breaker is the old order of playerOrder
        private void reorder(Comparator<Player> comparator) {
            Ordering<Player> ordering = Ordering.from(comparator);
            List<Player> newOrder = ordering.sortedCopy(playerOrder.view());
            playerOrder.setTo(newOrder);
            for (int i=0; i<newOrder.size(); i++) {
                Player player = newOrder.get(i);
                player.setIndex(i);
            }
        }
        
        public boolean isReverse() {
            return reverse.value();
        }
        
        public ArrayListState<Player> getPlayerOrder() {
            return playerOrder;
        }
        
        @Override
        public String toText() {
            // FIXME: This has to be checked if this returns the correct structure
            // and may be it is better to use another method instead of toText?
            return Util.joinWithDelimiter(playerOrder.view().toArray(new String[0]), ";");
        }
    }
    
    /**
     * @return number of players (non-bankrupt)
     */
    public static int getNumberOfActivePlayers(RailsItem item) {
        return item.getRoot().getPlayerManager().getNumberOfActivePlayers();
    }

}
