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
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Observable;
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
            playerModel.players.add(player);
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
        for (Player player:playerModel.players) {
            player.finishConfiguration();
        }
    }
    
    // sets initial priority player and certificate limits
    // TODO: rename method
    public void init() {
        playerModel.priorityPlayer.set(playerModel.players.get(0));
        int startCertificates = playerCertificateLimits.get(playerModel.players.size());
        playerCertificateLimit.set(startCertificates);
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }

    public ImmutableList<Player> getPlayers() {
        return playerModel.players.view();
    }

    public Player getPlayerByName(String name) {
        return playerNames.get(name);
    }

    public int getNumberOfPlayers() {
        return playerModel.players.size();
    }

    // dynamic getter/setters
    public GenericState<Player> getCurrentPlayerModel() {
        return playerModel.currentPlayer;
    }
    
    public Player getCurrentPlayer() {
        return playerModel.currentPlayer.value();
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
        playerModel.currentPlayer.set(player);
    }
    
    public Player getPriorityPlayer() {
        return playerModel.priorityPlayer.value();
    }
    
    public void setPriorityPlayer(Player player) {
        playerModel.priorityPlayer.set(player);
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
        Player priorityPlayer = playerModel.priorityPlayer.value();
        setCurrentPlayer(priorityPlayer);
        return priorityPlayer;
    }
    
    public Player setCurrentToNextPlayerAfter(Player player){
        Player nextPlayer = getNextPlayerAfter(player);
        setCurrentPlayer(nextPlayer);
        return nextPlayer;
    }
    
    public Player getNextPlayer() {
        return playerModel.getPlayerAfter(playerModel.currentPlayer.value());
    }
    
    public Player getNextPlayerAfter(Player player) {
        return playerModel.getPlayerAfter(player);
    }
    

    /**
     * @boolean include the current player at the start
     * @return a list of the next (active) players after the current player
     * (including/excluding the current player at the start)
     */
    public ImmutableList<Player> getNextPlayers(boolean include) {
        return getNextPlayersAfter(playerModel.currentPlayer.value(), include , false);
    }
    
    /**
     * @param boolean include the argument player at the start
     * @param boolean include the argument player at the end
     * @return a list of the next (active) players after the argument player
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
    
    // TODO: Check if this change is valid to set only non-bankrupt players
    // to be priority players
    public Player setPriorityPlayerToNext() {
        Player priorityPlayer = getNextPlayer();
        setPriorityPlayer(priorityPlayer);
        return priorityPlayer;
    }
    
    @Deprecated
    public Player getPlayerByIndex(int index) {
        return playerModel.players.get(index % getNumberOfPlayers());
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
       for (Player player:playerModel.players) {
           log.debug("New player "+ String.valueOf(++p) +" is "+player.getId() +
                   " (cash="+Bank.format(this, player.getCash())+")");
       }

       return playerModel.players.get(0);
    }
    
    public void reversePlayerOrder(boolean reverse) {
        playerModel.reverse.set(reverse);
    }

    public PlayerOrderModel getPlayerNamesModel() {
       return playerModel;
    }

    public static class PlayerOrderModel extends RailsModel {

        private final ArrayListState<Player> players = ArrayListState.create(this, "players");
        private final GenericState<Player> currentPlayer = GenericState.create(this, "currentPlayer");
        private final GenericState<Player> priorityPlayer = GenericState.create(this, "priorityPlayer");
        private final BooleanState reverse = BooleanState.create(this, "reverse");

        private PlayerOrderModel(PlayerManager parent, String id) {
            super(parent, id);
            reverse.set(false);
        }
    
        private Player getPlayerAfter(Player player) {
            int nextIndex = players.indexOf(player);
            do {
                if (reverse.value()) {
                    nextIndex = (nextIndex - 1 + players.size()) % players.size();
                } else {
                    nextIndex = (nextIndex + 1) % players.size();
                }
            } while (players.get(nextIndex).isBankrupt());
            return players.get(nextIndex);
        }
        
        // this creates a new order based on the comparator provided
        // last tie-breaker is the old order of players
        private void reorder(Comparator<Player> comparator) {
            Ordering<Player> ordering = Ordering.from(comparator);
            List<Player> newOrder = ordering.sortedCopy(players.view());
            players.setTo(newOrder);
        }
        
        public boolean isReverse() {
            return reverse.value();
        }
        
        @Override
        public String toText() {
            // FIXME: This has to be checked if this returns the correct structure
            // and may be it is better to use another method instead of toText?
            return Util.joinWithDelimiter(players.view().toArray(new String[0]), ";");
        }
    }

}
